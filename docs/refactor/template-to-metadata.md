# 供应商模板重构为元数据体系 — 设计文档

## 1. 背景与动机

### 1.1 现有问题

现有 `ProviderTemplate` 耦合了三类不同生命周期的数据：

| 数据类型 | 更新频率 | 示例 |
|---------|---------|------|
| 供应商身份 | 极低（几乎不变） | provider_id, provider_name, provider_type |
| 连接配置 | 低（按需调整） | base_url, timeout, max_retries |
| 模型元数据 | 高（频繁新增/下线/调价） | gpt-4.1, claude-sonnet-4-6, 定价, 上下文窗口 |

模型信息严重过时（如 moonshot.json 还是 v1-8k/32k/128k，而实际已是 kimi-k2.5+），且无法独立更新。每次更新模型信息都需要修改整个模板 JSON 并重新部署。

### 1.2 目标

1. **拆分实体**：`ProviderTemplate` → `ProviderMetadata`（供应商元数据）+ `ModelMetadata`（模型元数据），通过 `provider_id` 关联
2. **自动同步**：接入 Models.dev 社区数据源，实现模型元数据自动同步
3. **增量策略**：支持 BUILTIN/MODELS_DEV/PROVIDER_API/MANUAL/OVERRIDE 五种数据来源，按优先级决定覆盖策略
4. **独立更新**：模型元数据可独立 CRUD，不影响供应商配置

## 2. 数据模型设计

### 2.1 ER 关系

```
ProviderMetadata 1──N ModelMetadata
    (provider_id)    (provider_id)
```

- 关联方式：字符串 `provider_id`（如 "openai"），非外键
- 原因：`provider_id` 是业务自然键，跨表稳定，避免物理外键的级联约束

### 2.2 ProviderMetadata

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 自增主键 |
| provider_id | VARCHAR(64) UK | 供应商标识（"openai", "anthropic"） |
| provider_name | VARCHAR(128) | 显示名称 |
| provider_type | VARCHAR(32) | 供应商类型枚举（OPENAI），仅用于适配器选择 |
| provider_config | JSON | 连接配置 |
| metadata_type | VARCHAR(32) | OFFICIAL / USER |
| icon_url | VARCHAR(512) | 图标 URL |
| description | TEXT | 描述 |
| tags | JSON | 标签 |
| market_state | VARCHAR(32) | PRIVATE / PENDING / PUBLISHED / REJECTED |
| publish_at | TIMESTAMPTZ | 发布时间 |
| download_count | INT | 使用次数 |
| author_id | BIGINT | 作者 ID |
| author_name | VARCHAR(64) | 作者名称 |
| state | VARCHAR(32) | ACTIVE / DISABLED / DELETED |
| deleted_at | TIMESTAMPTZ | 逻辑删除时间 |
| created_at / updated_at | TIMESTAMPTZ | 审计字段 |
| created_by / updated_by | BIGINT | 审计字段 |

**与原 ProviderTemplate 的差异**：移除 `models_config`，模型元数据独立存储。

### 2.3 ModelMetadata

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL PK | 自增主键 |
| provider_id | VARCHAR(64) | 关联 ProviderMetadata |
| provider_model_id | VARCHAR(128) | 模型标识 |
| display_name | VARCHAR(128) | 显示名称 |
| model_family | VARCHAR(64) | 模型家族 |
| context_window | INTEGER | 上下文窗口 |
| max_input_tokens | INTEGER | 最大输入 Token |
| max_output_tokens | INTEGER | 最大输出 Token |
| input_price | DECIMAL(12,6) | 输入价格（$/M tokens） |
| output_price | DECIMAL(12,6) | 输出价格 |
| reasoning_price | DECIMAL(12,6) | 推理价格 |
| cache_read_price | DECIMAL(12,6) | 缓存读取价格 |
| cache_write_price | DECIMAL(12,6) | 缓存写入价格 |
| input_audio_price | DECIMAL(12,6) | 输入音频价格 |
| output_audio_price | DECIMAL(12,6) | 输出音频价格 |
| knowledge_cutoff | VARCHAR(32) | 知识截止日期 |
| release_date | DATE | 发布日期 |
| open_weights | BOOLEAN | 是否开源 |
| modalities | JSON | 模态列表 ["text","image","audio"] |
| capabilities | JSON | 能力 {"vision":true,"function_calling":true} |
| source | VARCHAR(32) | 数据来源 |
| source_synced_at | TIMESTAMPTZ | 同步时间 |
| state | VARCHAR(32) | ACTIVE / DISABLED / DEPRECATED |
| created_at / updated_at | TIMESTAMPTZ | 审计字段 |

**唯一键**：`(provider_id, provider_model_id)`

### 2.4 MetadataSource 枚举

| 值 | 优先级 | 说明 | 可被同步覆盖 |
|----|--------|------|-------------|
| BUILTIN | 最低 | 内置 JSON 文件 | 是 |
| MODELS_DEV | 低 | Models.dev API 同步 | 是 |
| PROVIDER_API | 中 | 供应商 API 获取 | 否 |
| MANUAL | 高 | 手动录入 | 否 |
| OVERRIDE | 最高 | 强制覆盖 | 否 |

同步策略：仅 `BUILTIN` 和 `MODELS_DEV` 来源的记录可被 Models.dev 同步更新，`MANUAL` 和 `OVERRIDE` 的记录不会被覆盖。

## 3. Models.dev 同步机制设计

### 3.1 数据源

- API URL: `https://models.dev/api.json`
- 数据格式：按 provider 组织，每个 provider 下包含 models 对象
- 同步频率：可配置，默认每天一次

### 3.2 同步流程

```
1. GET https://models.dev/api.json → JsonNode root
2. 遍历 root.providers，按 SUPPORTED_PROVIDERS 过滤
3. 对每个 (provider_id, model_id) 执行增量策略：
   a. DB 中不存在 → INSERT (source=MODELS_DEV)
   b. DB 中存在且 source=BUILTIN/MODELS_DEV → UPDATE
   c. DB 中存在且 source=MANUAL/OVERRIDE → SKIP
4. 标记 Models.dev 中消失的模型为 DEPRECATED：
   - 查找 DB 中 source=MODELS_DEV 且不在本次同步 keys 中的记录
   - 将其 state 标记为 DEPRECATED
```

### 3.3 配置项

```yaml
metadata:
  models-dev:
    api-url: https://models.dev/api.json
    sync-on-startup: false
    sync-interval: 86400
    enabled: true
  builtin:
    sync-on-startup: true
```

## 4. 内置 JSON 文件拆分方案

### 4.1 原结构（耦合）

```
templates/openai.json  →  { provider_config + models_config }
```

### 4.2 新结构（拆分）

```
metadata/
├── providers/
│   ├── openai.json       → 仅含 provider_id, provider_name, provider_type, provider_config, icon_url, description, tags
│   └── ...
└── models/
    ├── openai.json       → 仅含 models 数组
    └── ...
```

### 4.3 示例

`metadata/providers/openai.json`:
```json
{
  "provider_id": "openai",
  "provider_name": "OpenAI",
  "provider_type": "OPENAI",
  "provider_config": {
    "base_url": "https://api.openai.com",
    "website_url": "https://openai.com",
    "api_doc_url": "https://platform.openai.com/docs",
    "timeout": 60000,
    "max_retries": 3
  },
  "description": "OpenAI 官方 API",
  "icon_url": "https://cdn.example.com/icons/openai.png",
  "tags": ["国际", "多模态", "主流"]
}
```

`metadata/models/openai.json`:
```json
[
  {
    "provider_model_id": "gpt-4.1",
    "display_name": "GPT-4.1",
    "context_window": 1047576,
    "input_price": 2.0,
    "output_price": 8.0,
    "source": "BUILTIN",
    "capabilities": {"vision": true, "function_calling": true, "streaming": true}
  }
]
```

## 5. API 设计

### 5.1 供应商元数据 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/provider-metadata` | 分页查询 |
| GET | `/api/v1/provider-metadata/{id}` | 获取详情 |
| GET | `/api/v1/provider-metadata/official` | 获取所有官方元数据 |
| POST | `/api/v1/provider-metadata` | 创建自定义 |
| PUT | `/api/v1/provider-metadata/{id}` | 更新 |
| DELETE | `/api/v1/provider-metadata/{id}` | 删除（逻辑删除） |
| PATCH | `/api/v1/provider-metadata/{id}/market-state` | 更新市场状态 |
| POST | `/api/v1/provider-metadata/{id}/apply` | 应用元数据创建供应商 |

### 5.2 模型元数据 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/model-metadata` | 分页查询 |
| GET | `/api/v1/model-metadata/{id}` | 获取详情 |
| GET | `/api/v1/model-metadata/providers/{providerId}` | 查某供应商的所有模型 |
| POST | `/api/v1/model-metadata` | 创建 |
| PUT | `/api/v1/model-metadata/{id}` | 更新 |
| DELETE | `/api/v1/model-metadata/{id}` | 删除 |

### 5.3 同步 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/metadata-sync/all` | 全量同步 |
| POST | `/api/v1/metadata-sync/builtin` | 内置元数据同步 |
| POST | `/api/v1/metadata-sync/models-dev` | Models.dev 同步 |

## 6. 前端改造方案

### 6.1 类型定义

```typescript
// src/types/metadata.ts
export interface ProviderMetadata {
  id: number;
  providerId: string;           // 原 templateCode
  providerName: string;         // 原 templateName
  providerType: string;
  providerConfig: Record<string, unknown>;
  metadataType: 'OFFICIAL' | 'USER';
  iconUrl: string;
  description: string;
  tags: string[];
  marketState: MarketStatus;
  downloadCount: number;
  modelCount: number;
  state: MetadataState;
}

export interface ModelMetadata {
  id: number;
  providerId: string;
  providerModelId: string;
  displayName: string;
  modelFamily?: string;
  contextWindow?: number;
  inputPrice?: number;
  outputPrice?: number;
  source: MetadataSource;
  state: MetadataState;
}
```

### 6.2 页面改造

| 原组件 | 新组件 | 改造方向 |
|--------|--------|---------|
| `Templates/index.tsx` | `Metadata/index.tsx` | 供应商+模型元数据双 Tab 页面 |
| `Providers/ProviderTemplateSelector.tsx` | `Providers/ProviderMetadataSelector.tsx` | 从 ProviderMetadata 选择 |
| `Models/ModelTemplateSelector.tsx` | `Models/ModelMetadataSelector.tsx` | 从 ModelMetadata 选择 |

### 6.3 路由和权限

- `/templates` → `/metadata`
- `template:read` → `metadata:read`, `metadata:write`

## 7. 数据迁移策略

### 7.1 Migration SQL (V12)

1. 创建 `provider_metadata` 表
2. 创建 `model_metadata` 表
3. 从 `provider_templates` 迁移数据：
   - 每行 → 一行 provider_metadata + N 行 model_metadata
   - `template_code` → `provider_id`
   - `models_config` JSON 数组 → 逐条展开为 model_metadata 行
   - `source` 默认设为 `BUILTIN`

### 7.2 迁移后验证

- 确认 provider_metadata 行数 = 原 provider_templates 行数
- 确认 model_metadata 行数 = 原 models_config 中模型总数
- 确认关联关系正确（provider_id 一致）

## 8. 实施阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 0 | 编写设计文档 | ✅ 完成 |
| Phase 1 | 数据库 Migration (V12) | ✅ 完成 |
| Phase 2 | 领域层 Entity + Gateway 接口 | ✅ 完成 |
| Phase 3 | 基础设施层 DO + Repository + Gateway 实现 | ✅ 完成 |
| Phase 4 | 应用层 Service + DTO | ✅ 完成 |
| Phase 5 | 适配器层 Controller | ✅ 完成 |
| Phase 6 | 前端改造 | ✅ 完成 |
| Phase 7 | 测试补充 | 🔄 进行中 |
| Phase 8 | 清理旧代码 | ✅ 完成 |

## 9. 验证方案

1. **数据库迁移验证**：启动应用，确认 V12 migration 执行成功
2. **API 端点验证**：通过 curl 调用新 API，确认 CRUD 正常
3. **同步验证**：触发 `/api/v1/metadata-sync/builtin`，确认内置元数据加载成功
4. **Models.dev 同步验证**：触发 `/api/v1/metadata-sync/models-dev`，确认模型元数据从 API 同步成功
5. **前端验证**：启动前端，确认元数据页面正常，供应商创建向导正常
6. **构建验证**：`./mvnw clean install` 全量构建通过
7. **编译验证**：后端 279 源文件编译通过，前端 TypeScript 类型检查通过
