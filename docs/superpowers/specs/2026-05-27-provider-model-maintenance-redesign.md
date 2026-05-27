# 供应商页面模型维护重构设计

## 背景

协议重构设计（`2026-05-25-protocol-architecture-refactor-design.md`）明确要求 ModelSpec 去 `providerId`，成为全局模型注册表。当前供应商详情抽屉中独立的"模型规格"tab 需要相应调整，将模型关联下沉到渠道级别。

## 核心变更

ModelSpec 全局化：`Provider —→ Channel —→ ChannelModel —→ ModelSpec（全局）`

| 变更项 | 说明 |
|--------|------|
| ModelSpec | 去掉 `providerId`，成为全局模型注册表，按 `providerModelId` 唯一 |
| 模型来源 | Catalog 同步为主要途径，DataInitializer 预置为辅 |
| 供应商-模型关系 | 通过 `Provider → Channels → ChannelModels → ModelSpecs` 间接关联 |
| 供应商卡片模型数 | 统计该供应商所有渠道关联的去重 ModelSpec 数 |

## 供应商页面调整

### 去掉模型规格 Tab

- 供应商详情抽屉中移除"模型规格"（RobotOutlined）tab
- 仅保留"基本信息"和"渠道"两个 tab

### 渠道 Tab 展开行增加 Models 面板

展开行结构由当前两个 tab 变为三个：

```
渠道行展开
├── Endpoints (端点管理) — 不变
├── Credentials (凭证管理) — 不变
└── Models (关联模型) — 新增
```

Models 面板内容：

| 字段 | 说明 |
|------|------|
| 模型标识 | `providerModelId`（如 `gpt-4o`） |
| 显示名 | `displayName` |
| 模型系列 | `modelFamily` |
| 状态 | ACTIVE / INACTIVE |
| 操作 | 解绑 / 启停 |

操作：
- **关联模型**：搜索全局 ModelSpec 列表，选择后创建 ChannelModel（定价字段后端默认填充）
- **解绑**：删除 ChannelModel 记录
- **启停**：切换 ChannelModel state

注意：不展示/编辑定价字段，仅处理关联关系。

## API 调整

### 移除的 API

| 方法 | 路径 | 原因 |
|------|------|------|
| GET | `/api/providers/{id}/model-specs` | ModelSpec 不再按 provider 查询 |
| POST | `/api/providers/{id}/model-specs` | 模型创建不依赖 provider |
| PUT | `/api/providers/{id}/model-specs/{specId}` | 同上 |
| PATCH | `/api/providers/{id}/model-specs/{specId}/state` | 同上 |

### 保留/新增的 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/model-specs` | 全局模型列表，支持 `search` 参数按 `providerModelId`/`displayName` 过滤 |
| GET | `/api/channels/{channelId}/models` | **新增** — 查询渠道关联模型列表（返回 ChannelModel + ModelSpec 展平） |
| POST | `/api/channels/{channelId}/models` | **新增** — 关联全局模型到渠道 |
| DELETE | `/api/channels/{channelId}/models/{id}` | **新增** — 解绑关联 |
| PATCH | `/api/channels/{channelId}/models/{id}/state` | **新增** — 启停关联 |

## 后端调整

- `ModelSpec` 实体删除 `providerId` 字段
- `ModelSpecGateway` 删除 `findByProviderId()` 方法
- `ModelSpecService` 所有方法去掉 `providerId` 参数
- 新增 `ChannelModelService` 处理 `channel_models` 的 CRUD
- `CatalogSyncService` 中 ModelSpec 创建逻辑去掉 providerId
- `DataInitializer.createModelSpec()` 去掉 providerId 参数

## 存量数据迁移

1. 多个 Provider 下相同 `providerModelId` 的 ModelSpec 合并为一条全局记录
2. `channel_models` 中 `model_spec_id` 更新为合并后的 ID
3. DDL：`ALTER TABLE model_specs DROP COLUMN provider_id`
4. 新增唯一约束：`UNIQUE(provider_model_id)`