# Provider 模板功能设计

> **文档版本**: 1.0
> **创建日期**: 2026-05-07
> **状态**: 待审阅

---

## 一、概述

### 1.1 功能目标

预置常见国内外大模型厂商及其模型模板，用户只需设置 API Key 即可快速创建 Provider 配置。

### 1.2 目标用户

管理员快速搭建网关配置，减少手动配置工作量。

### 1.3 核心价值

- **开箱即用**：官方预置主流 Provider，无需手动配置
- **实时更新**：官方模板通过 Git 仓库维护，支持热更新
- **社区贡献**：支持用户创建、分享自定义模板
- **离线可用**：内置核心 Provider 模板，无网络也能使用

---

## 二、功能范围

### 2.1 核心功能

| 功能 | 说明 | 优先级 |
|------|------|--------|
| 官方模板同步 | 从 Git 仓库同步官方模板到数据库 | P0 |
| 模板列表查询 | 支持按类型、Provider 类型、关键词筛选 | P0 |
| 模板详情查看 | 预览 Provider 信息和模型列表 | P0 |
| 应用模板创建 Provider | 一键创建 Provider + Channel + Models + ApiKey | P0 |
| 用户创建自定义模板 | 从现有 Provider 提取或手动创建 | P1 |
| 导入导出 JSON | 文件形式分享模板 | P1 |
| 公共模板市场 | 浏览、搜索、使用公共模板 | P1 |
| 发布审核流程 | 自动检测 + 可选管理员审核 | P1 |

### 2.2 不做清单

- 模板版本管理（后续迭代）
- 模板评分评论系统（后续迭代）
- 模板使用统计分析（后续迭代）

---

## 三、数据模型

### 3.1 新增表：`provider_templates`

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| `id` | BIGINT | PK | 主键 |
| `template_code` | VARCHAR(64) | UNIQUE, NOT NULL | 模板唯一标识 |
| `template_name` | VARCHAR(128) | NOT NULL | 模板显示名称 |
| `template_type` | VARCHAR(32) | NOT NULL | OFFICIAL（官方）/ USER（用户） |
| `provider_type` | VARCHAR(32) | NOT NULL | OPENAI/ANTHROPIC/GEMINI/ZHIPU/QWEN/VOLCENGINE/WENXIN/OTHER |
| `provider_config` | JSONB | NOT NULL | Provider 配置 |
| `models_config` | JSONB | NOT NULL | 模型列表配置 |
| `author_id` | BIGINT | FK, NULL | 创建者 ID（官方模板为 NULL） |
| `author_name` | VARCHAR(64) | NULL | 创建者名称 |
| `market_status` | VARCHAR(32) | NOT NULL | PRIVATE/PENDING/PUBLISHED/REJECTED |
| `publish_at` | TIMESTAMP WITH TIME ZONE | NULL | 发布时间 |
| `download_count` | INT | NOT NULL, DEFAULT 0 | 使用次数 |
| `tags` | JSONB | NULL | 标签数组 |
| `description` | TEXT | NULL | 模板描述 |
| `icon_url` | VARCHAR(512) | NULL | 图标 URL |
| `status` | VARCHAR(32) | NOT NULL | ACTIVE/DISABLED |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | 创建时间 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | 更新时间 |
| `deleted_at` | TIMESTAMP WITH TIME ZONE | NULL | 软删除时间 |

**索引设计**：

```sql
CREATE UNIQUE INDEX uq_provider_templates_code ON provider_templates(template_code) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_type ON provider_templates(template_type, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_provider ON provider_templates(provider_type) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_market ON provider_templates(market_status) WHERE deleted_at IS NULL;
CREATE INDEX ix_provider_templates_author ON provider_templates(author_id) WHERE deleted_at IS NULL;
```

### 3.2 命名规范

- **template_code**：小写 + 连字符，如 `openai`、`deepseek`、`qwen-max`
- **template_name**：显示名称，如 `OpenAI`、`DeepSeek`、`通义千问`
- 官方模板的 `template_code` 与 `provider_type` 保持一致（小写形式）

### 3.3 provider_config 结构

```json
{
  "provider_name": "OpenAI",
  "provider_type": "OPENAI",
  "base_url": "https://api.openai.com",
  "website_url": "https://openai.com",
  "api_doc_url": "https://platform.openai.com/docs"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `provider_name` | string | 是 | Provider 显示名称 |
| `provider_type` | string | 是 | Provider 类型枚举 |
| `base_url` | string | 是 | API Base URL |
| `website_url` | string | 否 | 官网地址 |
| `api_doc_url` | string | 否 | API 文档地址 |

### 3.3 models_config 结构

```json
[
  {
    "provider_model_id": "gpt-4o",
    "display_name": "GPT-4o",
    "context_window": 128000,
    "input_price": 2.5,
    "output_price": 10.0,
    "capabilities": {
      "vision": true,
      "function_calling": true,
      "streaming": true
    }
  }
]
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `provider_model_id` | string | 是 | Provider 侧的模型 ID |
| `display_name` | string | 是 | 显示名称 |
| `context_window` | int | 是 | 上下文窗口大小 |
| `input_price` | decimal | 否 | 输入价格（美元/1K tokens） |
| `output_price` | decimal | 否 | 输出价格（美元/1K tokens） |
| `capabilities` | object | 否 | 能力标签 |

### 3.4 tags 结构

```json
["国际", "多模态", "主流", "低成本"]
```

### 3.5 枚举定义

#### TemplateType（模板类型）

| 值 | 说明 |
|----|------|
| `OFFICIAL` | 官方预置模板 |
| `USER` | 用户自定义模板 |

#### MarketStatus（市场状态）

| 值 | 说明 |
|----|------|
| `PRIVATE` | 私有，仅创建者可见 |
| `PENDING` | 待审核 |
| `PUBLISHED` | 已发布到公共市场 |
| `REJECTED` | 审核拒绝 |

---

## 四、系统架构

### 4.1 组件架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         管理控制台                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ 模板列表页面 │  │ 模板详情页面 │  │ 模板创建/编辑页面       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Adapter 层                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ProviderTemplateController                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Application 层                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ProviderTemplateService                                   │   │
│  │ - 模板 CRUD、导入导出、发布、应用                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ OfficialTemplateSyncService                               │   │
│  │ - 从 Git 仓库同步官方模板到数据库                         │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Domain 层                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ProviderTemplate (Entity)                                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ProviderTemplateGateway (Interface)                       │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Infrastructure 层                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ ProviderTemplateGatewayImpl                               │   │
│  └──────────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ GitTemplateRepository                                     │   │
│  │ - 从 Git 仓库克隆/拉取模板                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 包结构

```
com.codingas.gateway
├── adapter/
│   └── admin/
│       ├── controller/
│       │   └── ProviderTemplateController.java
│       └── dto/
│           ├── TemplateCreateRequest.java
│           ├── TemplateUpdateRequest.java
│           ├── TemplateResponse.java
│           ├── TemplateApplyRequest.java
│           └── TemplateApplyResponse.java
├── application/
│   └── template/
│       ├── ProviderTemplateService.java
│       └── OfficialTemplateSyncService.java
├── domain/
│   └── template/
│       ├── entity/
│       │   └── ProviderTemplate.java
│       ├── gateway/
│       │   └── ProviderTemplateGateway.java
│       └── service/
│           └── TemplateValidator.java
└── infrastructure/
    └── template/
        ├── gateway/
        │   └── ProviderTemplateGatewayImpl.java
        ├── repository/
        │   └── GitTemplateRepository.java
        └── config/
            └── TemplateSyncConfig.java
```

---

## 五、Git 模板仓库设计

### 5.1 仓库结构

```
llm-gateway-templates/
├── templates/
│   ├── openai.json
│   ├── anthropic.json
│   ├── gemini.json
│   ├── zhipu.json
│   ├── qwen.json
│   ├── deepseek.json
│   ├── moonshot.json
│   ├── baidu.json
│   ├── minimax.json
│   └── ...
├── schemas/
│   └── template.schema.json
└── README.md
```

### 5.2 模板文件示例 (openai.json)

```json
{
  "template_code": "openai",
  "template_name": "OpenAI",
  "provider_type": "OPENAI",
  "provider_config": {
    "provider_name": "OpenAI",
    "base_url": "https://api.openai.com",
    "website_url": "https://openai.com",
    "api_doc_url": "https://platform.openai.com/docs"
  },
  "models_config": [
    {
      "provider_model_id": "gpt-4o",
      "display_name": "GPT-4o",
      "context_window": 128000,
      "input_price": 2.5,
      "output_price": 10.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "gpt-4-turbo",
      "display_name": "GPT-4 Turbo",
      "context_window": 128000,
      "input_price": 10.0,
      "output_price": 30.0,
      "capabilities": {
        "vision": true,
        "function_calling": true,
        "streaming": true
      }
    },
    {
      "provider_model_id": "gpt-3.5-turbo",
      "display_name": "GPT-3.5 Turbo",
      "context_window": 16385,
      "input_price": 0.5,
      "output_price": 1.5,
      "capabilities": {
        "vision": false,
        "function_calling": true,
        "streaming": true
      }
    }
  ],
  "description": "OpenAI 官方 API，支持 GPT-4o、GPT-4 Turbo、GPT-3.5 等模型",
  "icon_url": "https://cdn.example.com/icons/openai.png",
  "tags": ["国际", "多模态", "主流"]
}
```

### 5.3 内置官方模板清单（首批）

| Provider | template_code | 说明 |
|----------|---------------|------|
| OpenAI | `openai` | GPT-4o、GPT-4 Turbo、GPT-3.5 |
| Anthropic | `anthropic` | Claude Sonnet、Claude Opus、Claude Haiku |
| Google | `gemini` | Gemini Pro、Gemini Flash |
| 智谱 AI | `zhipu` | GLM-4、GLM-4-Air |
| 通义千问 | `qwen` | Qwen-Max、Qwen-Plus、Qwen-Turbo |
| DeepSeek | `deepseek` | DeepSeek-V3、DeepSeek-Coder |
| 月之暗面 | `moonshot` | Moonshot-V1 |
| 百度 | `wenxin` | ERNIE-4.0、ERNIE-3.5 |
| MiniMax | `minimax` | abab6.5、abab5.5 |

---

## 六、API 设计

### 6.1 端点总览

| 端点 | 方法 | 说明 | 权限 |
|------|------|------|------|
| `/api/v1/templates` | GET | 分页查询模板列表 | Admin |
| `/api/v1/templates/{id}` | GET | 获取模板详情 | Admin |
| `/api/v1/templates` | POST | 创建自定义模板 | Admin |
| `/api/v1/templates/{id}` | PUT | 更新模板 | Admin |
| `/api/v1/templates/{id}` | DELETE | 删除模板 | Admin |
| `/api/v1/templates/{id}/publish` | POST | 发布到公共市场 | Admin |
| `/api/v1/templates/import` | POST | 导入 JSON 模板 | Admin |
| `/api/v1/templates/{id}/export` | GET | 导出为 JSON 文件 | Admin |
| `/api/v1/templates/{id}/apply` | POST | 应用模板创建 Provider | Admin |
| `/api/v1/templates/sync` | POST | 手动同步官方模板 | Admin |

### 6.2 查询模板列表

**请求**：
```
GET /api/v1/templates?type=OFFICIAL&provider_type=OPENAI&keyword=gpt&page=1&limit=20
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `type` | string | 否 | OFFICIAL/USER/MARKET，默认返回全部 |
| `provider_type` | string | 否 | 按 Provider 类型过滤 |
| `keyword` | string | 否 | 搜索模板名称 |
| `page` | int | 否 | 页码，默认 1 |
| `limit` | int | 否 | 每页数量，默认 20，最大 100 |

**响应**：
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "template_code": "openai",
        "template_name": "OpenAI",
        "template_type": "OFFICIAL",
        "provider_type": "OPENAI",
        "description": "OpenAI 官方 API",
        "icon_url": "https://cdn.example.com/icons/openai.png",
        "tags": ["国际", "多模态", "主流"],
        "model_count": 5,
        "download_count": 1234,
        "author_name": "Official",
        "created_at": "2026-05-01T00:00:00Z",
        "updated_at": "2026-05-07T00:00:00Z"
      }
    ],
    "total": 1,
    "page": 1,
    "limit": 20
  }
}
```

### 6.3 获取模板详情

**请求**：
```
GET /api/v1/templates/{id}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 1,
    "template_code": "openai",
    "template_name": "OpenAI",
    "template_type": "OFFICIAL",
    "provider_type": "OPENAI",
    "provider_config": {
      "provider_name": "OpenAI",
      "base_url": "https://api.openai.com",
      "website_url": "https://openai.com",
      "api_doc_url": "https://platform.openai.com/docs"
    },
    "models_config": [
      {
        "provider_model_id": "gpt-4o",
        "display_name": "GPT-4o",
        "context_window": 128000,
        "input_price": 2.5,
        "output_price": 10.0,
        "capabilities": {
          "vision": true,
          "function_calling": true,
          "streaming": true
        }
      }
    ],
    "description": "OpenAI 官方 API",
    "icon_url": "https://cdn.example.com/icons/openai.png",
    "tags": ["国际", "多模态", "主流"],
    "author_id": null,
    "author_name": "Official",
    "market_status": "PUBLISHED",
    "download_count": 1234,
    "created_at": "2026-05-01T00:00:00Z",
    "updated_at": "2026-05-07T00:00:00Z"
  }
}
```

### 6.4 创建自定义模板

**请求**：
```
POST /api/v1/templates
```

```json
{
  "template_code": "my-custom-provider",
  "template_name": "我的自定义 Provider",
  "provider_type": "OTHER",
  "provider_config": {
    "provider_name": "Custom Provider",
    "base_url": "https://api.custom.com",
    "website_url": "https://custom.com"
  },
  "models_config": [
    {
      "provider_model_id": "model-v1",
      "display_name": "Model V1",
      "context_window": 32000,
      "capabilities": {
        "vision": false,
        "function_calling": true
      }
    }
  ],
  "description": "自定义模板描述",
  "tags": ["自定义"]
}
```

**响应**：
```json
{
  "success": true,
  "data": {
    "id": 10,
    "template_code": "my-custom-provider"
  }
}
```

### 6.5 应用模板创建 Provider

**请求**：
```
POST /api/v1/templates/{id}/apply
```

```json
{
  "api_key": "sk-xxxxxxxxxxxxxxxx",
  "channel_name": "我的 OpenAI 渠道",
  "channel_priority": 100,
  "channel_weight": 100,
  "selected_models": ["gpt-4o", "gpt-4-turbo"]
}
```

**请求参数说明**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `api_key` | string | 是 | Provider API Key |
| `channel_name` | string | 否 | 渠道名称，默认使用模板名称 |
| `channel_priority` | int | 否 | 渠道优先级，默认 100 |
| `channel_weight` | int | 否 | 渠道权重，默认 100 |
| `selected_models` | array | 否 | 选择的模型 ID 列表，默认使用模板全部模型 |

**响应**：
```json
{
  "success": true,
  "data": {
    "provider_id": 1,
    "channel_id": 1,
    "api_key_id": 1,
    "model_ids": [1, 2]
  }
}
```

### 6.6 导入模板

**请求**：
```
POST /api/v1/templates/import
Content-Type: multipart/form-data

file: template.json
```

**响应**：
```json
{
  "success": true,
  "data": {
    "template_id": 10,
    "template_code": "imported-provider",
    "template_name": "Imported Provider"
  }
}
```

### 6.7 导出模板

**请求**：
```
GET /api/v1/templates/{id}/export
```

**响应**：
```
Content-Type: application/json
Content-Disposition: attachment; filename="openai-template.json"

{
  "template_code": "openai",
  "template_name": "OpenAI",
  ...
}
```

### 6.8 发布到公共市场

**请求**：
```
POST /api/v1/templates/{id}/publish
```

**响应**：
```json
{
  "success": true,
  "data": {
    "template_id": 10,
    "market_status": "PUBLISHED"
  }
}
```

**说明**：如果自动检测发现可疑内容，`market_status` 将为 `PENDING`，等待管理员审核。

### 6.9 手动同步官方模板

**请求**：
```
POST /api/v1/templates/sync
```

**响应**：
```json
{
  "success": true,
  "data": {
    "synced_count": 5,
    "updated_count": 2,
    "added_count": 3,
    "synced_at": "2026-05-07T10:00:00Z"
  }
}
```

---

## 七、使用流程

### 7.1 管理员使用模板创建 Provider

```
1. 浏览模板列表
   ├── 官方模板（从远程同步）
   ├── 公共市场模板（用户发布）
   └── 我的模板（用户自建）

2. 选择模板 → 预览详情
   ├── Provider 信息
   ├── 包含的模型列表
   └── 描述、标签、使用次数

3. 填写 API Key → 一键创建
   ├── 自动创建 Provider
   ├── 自动创建 Channel（使用默认配置）
   ├── 自动创建 ProviderApiKey（关联到 Channel）
   └── 自动创建 Models（关联到 Provider）

4. 可选：调整配置
   ├── 修改 Channel 优先级、权重等
   ├── 增减模型
   └── 配置路由策略
```

### 7.2 用户创建自定义模板

```
1. 从现有 Provider 创建模板
   ├── 选择已配置的 Provider
   ├── 自动提取 Provider 信息和模型列表
   └── 用户补充描述、标签

2. 手动创建模板
   ├── 填写 Provider 信息
   ├── 添加模型列表
   └── 保存为私有模板

3. 发布到公共市场（可选）
   ├── 系统自动检测格式和安全性
   ├── 检测通过 → 发布
   └── 检测可疑 → 等待管理员审核
```

---

## 八、安全与校验

### 8.1 模板导入安全检测

| 检测项 | 规则 | 处理方式 |
|--------|------|---------|
| 格式校验 | JSON 格式正确，符合 Schema 定义 | 不通过则拒绝导入 |
| 必填字段 | template_code、template_name、provider_type、provider_config、models_config | 不通过则拒绝导入 |
| template_code 唯一性 | 不能与已存在的模板重复 | 不通过则拒绝导入 |
| Base URL 格式 | 必须是有效的 HTTPS URL | 不通过则拒绝导入 |
| 敏感词过滤 | 模板名称、描述不能包含敏感词 | 不通过则拒绝导入 |
| 恶意代码检测 | JSON 不能包含脚本注入 | 可疑则标记待审核 |

### 8.2 发布审核流程

```
用户提交发布
      │
      ▼
┌─────────────────┐
│ 自动安全检测     │
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
 通过       可疑
    │         │
    ▼         ▼
 自动发布   标记 PENDING
            │
            ▼
       管理员审核
            │
       ┌────┴────┐
       ▼         ▼
     通过       拒绝
       │         │
       ▼         ▼
    PUBLISHED  REJECTED
```

### 8.3 权限控制

| 操作 | 官方模板 | 用户私有模板 | 公共市场模板 |
|------|---------|-------------|-------------|
| 查看 | ✅ | ✅（仅自己） | ✅ |
| 使用 | ✅ | ✅（仅自己） | ✅ |
| 编辑 | ❌ | ✅（仅自己） | ❌ |
| 删除 | ❌ | ✅（仅自己） | ❌ |
| 发布 | ❌ | ✅（仅自己） | ❌ |

---

## 九、配置项

### 9.1 同步配置

```yaml
template:
  git:
    url: https://github.com/codingas/llm-gateway-templates.git
    branch: main
    local-path: ${user.home}/.llm-gateway/templates
    sync-on-startup: true
    sync-interval: 3600  # 秒，0 表示禁用定时同步
```

### 9.2 内置模板与 Git 同步的关系

| 场景 | 数据来源 | 说明 |
|------|---------|------|
| 首次启动 | 内置模板 | 代码中的 JSON 文件，保证核心 Provider 可用 |
| Git 同步成功 | Git 仓库 | 覆盖内置模板，获取最新数据 |
| Git 同步失败 | 本地缓存 | 使用上次同步的数据 |
| 无网络 + 无缓存 | 内置模板 | 兜底方案 |

**同步优先级**：
1. Git 仓库（最新）
2. 本地缓存
3. 内置模板（兜底）

---

## 十、测试策略

### 10.1 单元测试

| 测试范围 | 测试内容 | 覆盖率要求 |
|---------|---------|-----------|
| `ProviderTemplateService` | 模板 CRUD、导入导出、应用模板 | ≥90% |
| `OfficialTemplateSyncService` | Git 同步、解析 JSON、去重合并 | ≥90% |
| `GitTemplateRepository` | 克隆、拉取、解析、缓存 | ≥85% |
| `TemplateValidator` | 格式校验、安全检测 | ≥90% |

### 10.2 集成测试

| 测试场景 | 测试内容 |
|---------|---------|
| 应用模板创建 Provider | 验证 Provider、Channel、Model、ApiKey 正确创建 |
| 导入导出一致性 | 导出后重新导入，数据一致 |
| Git 同步流程 | 模拟 Git 仓库，验证同步正确 |
| 发布审核流程 | 验证自动检测和审核状态流转 |

### 10.3 E2E 测试

| 测试场景 | 验证点 |
|---------|-------|
| 管理员使用官方模板 | 浏览 → 选择 → 填写 API Key → 创建成功 → 调用模型成功 |
| 用户创建自定义模板 | 从现有 Provider 创建 → 发布到市场 → 其他用户使用 |
| 导入导出模板 | 导出 JSON → 下载 → 导入 → 使用成功 |

---

## 十一、技术依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| JGit | 6.x | 克隆/拉取 Git 仓库 |
| Jackson | 2.x | JSON 解析 |
| JSON Schema Validator | - | 模板格式校验 |

---

## 十二、实施计划

### 12.1 分阶段实施

| 阶段 | 功能 | 工作量估算 |
|------|------|-----------|
| Phase 1 | 数据库表 + 实体 + Gateway 接口 | 1 天 |
| Phase 2 | Git 同步服务 + 内置模板 | 2 天 |
| Phase 3 | 模板 CRUD API | 1 天 |
| Phase 4 | 应用模板创建 Provider | 1 天 |
| Phase 5 | 导入导出功能 | 1 天 |
| Phase 6 | 公共市场 + 发布审核 | 2 天 |
| Phase 7 | 前端页面 | 2 天 |
| Phase 8 | 测试 + 文档 | 2 天 |

**总计：约 12 人天**

---

## 十三、变更记录

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0 | 2026-05-07 | 初始版本 |
