# Catalog 体系完善设计规格

> **定位**：Catalog = 外部世界的只读快照，回答"有什么可用"。与运营实体通过一次性物化对接，物化后运营实体独立演进。

## 实体关系

```
ProviderCatalog (1) ──→ (N) PlanCatalog
                            │
                       (N) ──→ (N) PlanModelCatalog ──→ (1) ModelSpecCatalog
```

四层与运营层同构：

| Catalog 层 | 运营层 | 职责 |
|---|---|---|
| ProviderCatalog | Provider | 谁在卖 |
| PlanCatalog | Channel + ChannelEndpoint | 卖什么套餐（含套餐价格、计费模式、端点、包含的模型列表） |
| PlanModelCatalog | ChannelModel | 套餐包含哪些模型（纯关联） |
| ModelSpecCatalog | ModelSpec | 模型能力描述 |

与旧 domain/metadata 的映射：

| 旧实体 | 新实体 | 变化 |
|---|---|---|
| ProviderMetadata | ProviderCatalog | 去掉 providerConfig/tags |
| ModelMetadata | ModelSpecCatalog | 只保留能力描述 |
| ProductMetadata | PlanCatalog | Product→Plan，endpoints 从 Map→JSON |
| ProductModelMetadata | PlanModelCatalog | 纯关联行，定价在 PlanCatalog.pricing |

## 实体字段

### ProviderCatalog

| 字段 | 类型 | 说明 |
|---|---|---|
| providerCode | String | 业务标识：openai, volcengine, deepseek |
| providerName | String | 展示名 |
| providerType | Enum | INTERNATIONAL / DOMESTIC |
| logoUrl | String | |
| websiteUrl | String | |
| baseUrl | String | 厂商默认 API 地址（参考值） |
| description | String | |
| source | CatalogSource | |
| syncedAt | Instant | |
| state | CatalogState | |

### PlanCatalog

| 字段 | 类型 | 说明 |
|---|---|---|
| planCode | String | 业务标识：volcengine_doubao_payg |
| providerCode | String | → ProviderCatalog |
| planName | String | 展示名 |
| billingMode | Enum | PAY_AS_YOU_GO / SUBSCRIPTION / PACKAGE |
| endpoints | JSON | [{protocol, url}, ...] |
| pricing | JSON | [{providerModelId, inputPrice, outputPrice, ...}, ...] |
| description | String | |
| source | CatalogSource | |
| syncedAt | Instant | |
| state | CatalogState | |

### PlanModelCatalog

| 字段 | 类型 | 说明 |
|---|---|---|
| planCode | String | → PlanCatalog |
| providerModelId | String | → ModelSpecCatalog |
| source | CatalogSource | |
| syncedAt | Instant | |
| state | CatalogState | |

### ModelSpecCatalog

| 字段 | 类型 | 说明 |
|---|---|---|
| providerModelId | String | 模型标识 |
| displayName | String | |
| modelFamily | String | 模型族 |
| contextWindow | Integer | |
| maxInputTokens | Integer | |
| maxOutputTokens | Integer | |
| knowledgeCutoff | String | 知识截止日期 |
| capabilities | JSON | {vision, tool_use, streaming, ...} |
| modalities | JSON | ["text", "image", "audio"] |
| source | CatalogSource | |
| syncedAt | Instant | |
| state | CatalogState | |

### CatalogSource 枚举

优先级（低→高）：`BUILTIN < MODELS_DEV < PROVIDER_API < MANUAL < OVERRIDE`

低优先级不可覆盖高优先级，同优先级可互相覆盖。

### CatalogState 枚举

`ACTIVE / DEPRECATED`

## CatalogDomainService 核心逻辑

### 同步

| 数据源 | 触发方式 | 实现 |
|---|---|---|
| BUILTIN | 应用启动时 | 读 classpath `catalog/*.json` |
| MODELS_DEV | 定时 / 手动 API | 调 Models.dev API，防腐层隔离 |
| MANUAL | 管理后台手动录入 | 直接 save |
| OVERRIDE | 用户基于同步数据修改 | 直接 save，source 标 OVERRIDE |

```
syncFromSource(source):
  1. 拉取数据
  2. 逐条 upsert
  3. markDeprecated(source, 本轮出现的 uniqueKey 集合)
  4. 返回 SyncResult(added, updated, deprecated, skipped)
```

### 去重 + upsert

| 实体 | 唯一键 |
|---|---|
| ProviderCatalog | providerCode |
| PlanCatalog | planCode |
| PlanModelCatalog | (planCode, providerModelId) |
| ModelSpecCatalog | providerModelId |

```
upsert(catalog):
  existing = findByUniqueKey(catalog)
  if null → insert, return ADDED
  if catalog.source.priority > existing.source.priority → overwrite, return UPDATED
  if catalog.source.priority == existing.source.priority → overwrite, return UPDATED
  if catalog.source.priority < existing.source.priority → skip, return SKIPPED
```

### markDeprecated

```
markDeprecated(source, activeKeys):
  for entry in findBySource(source):
      if entry.uniqueKey not in activeKeys AND entry.state == ACTIVE:
          entry.state = DEPRECATED
```

上游消失 → DEPRECATED，不删除。前端展示为"已下线"。

### 物化（materialize）

一次性操作，物化后运营实体独立演进，catalog 更新不自动推送。

```
materializeProvider(providerCode):
  catalog = findByCode(providerCode)
  if Provider.existsByCode → throw "已物化"
  new Provider(...) → save

materializePlan(planCode):
  catalog = findByCode(planCode)
  if Channel.existsByName → throw "已物化"
  new Channel(...) → save
  for endpoint in catalog.endpoints:
      new ChannelEndpoint(channel.id, protocol, url) → save
  for pricing in catalog.pricing:
      modelSpec = findOrCreateModelSpec(pricing.providerModelId)
      new ChannelModel(channel.id, modelSpec.id, pricing.*) → save

materializeModelSpec(providerModelId):
  catalog = findByProviderModelId(providerModelId)
  if ModelSpec.existsByProviderModelId → throw "已物化"
  new ModelSpec(...) → save
```

findOrCreateModelSpec：物化 Plan 时如果 ModelSpec 不存在就自动创建，这是唯一允许的级联物化。

### 查询

```
listProviderCatalogs(providerType?, keyword?)
listPlanCatalogs(providerCode?)
getPlanDetail(planCode)           // PlanCatalog + pricing + endpoints
listModelSpecCatalogs(providerCode?, keyword?, capability?)
listUnmaterialized(providerCode?) // 未物化的 catalog 条目
```

## 旧 metadata 清理

### 删除

| 删除目标 | 说明 |
|---|---|
| domain/metadata/entity/* | 4 个旧实体 |
| domain/metadata/enums/* | MetadataState, MetadataSource |
| domain/metadata/gateway/* | 5 个旧 Gateway 接口 |
| domain/metadata/service/* | 旧 DomainService |
| infrastructure/metadata/* | 全部旧实现 |
| application/metadata/* | 旧应用服务 + DTO |
| adapter/api/*MetadataController | 旧 Controller |

### 新增 infrastructure/supply/catalog

| 新增 | 说明 |
|---|---|
| ProviderCatalogDo | JPA 实体 |
| PlanCatalogDo | JPA 实体 |
| PlanModelCatalogDo | JPA 实体 |
| ModelSpecCatalogDo | JPA 实体 |
| *Repository | 4 个 JPA Repository |
| *GatewayImpl | 4 个 Gateway 实现 |
| BuiltinCatalogLoader | 读 classpath JSON |
| ModelsDevSyncClient | 调 Models.dev API |
| CatalogSyncScheduler | 定时同步调度 |

### 新增 application/catalog

| 新增 | 说明 |
|---|---|
| CatalogService | 接口 |
| CatalogServiceImpl | 实现 |
| CatalogSyncService | 同步编排 |
| CatalogMaterializeService | 物化编排 |
| dto/* | 请求/响应 DTO |

### 新增 adapter/api

| 新增 | 说明 |
|---|---|
| CatalogController | /api/v1/catalog/** |

### Flyway 迁移

- V38：建 4 张 catalog 表 + 删旧 metadata 表

## BUILTIN 数据文件

从 `src/main/resources/metadata/` 下的旧 JSON 和调研报告数据，生成新的 `src/main/resources/catalog/` 目录结构：

```
catalog/
  providers.json       — 所有 ProviderCatalog 条目
  plans.json           — 所有 PlanCatalog 条目
  plan-models.json     — 所有 PlanModelCatalog 条目
  model-specs.json     — 所有 ModelSpecCatalog 条目
```

### 数据来源与转换规则

#### providers.json — 来自 metadata/providers/*.json + 调研报告

旧 providers JSON 字段映射：

| 旧字段 | 新字段 | 转换 |
|---|---|---|
| provider_id | providerCode | 直接映射 |
| provider_config.provider_name | providerName | 从 provider_config 中提取 |
| provider_config.base_url | baseUrl | 从 provider_config 中提取 |
| provider_config.website_url | websiteUrl | 从 provider_config 中提取 |
| icon_url | logoUrl | 直接映射 |
| description | description | 直接映射 |
| tags 中的"国际"/"国内" | providerType | 含"国际"→INTERNATIONAL，含"国内"→DOMESTIC |
| — | source | 固定 "BUILTIN" |
| — | syncedAt | 固定 null |
| — | state | 固定 "ACTIVE" |

新增供应商（metadata 中无但调研报告中有）：

| providerCode | providerName | providerType | baseUrl |
|---|---|---|---|
| stepfun | 阶跃星辰 | DOMESTIC | https://api.stepfun.com/v1 |
| 360zhinao | 360智脑 | DOMESTIC | https://api.360.cn/v1 |
| minimax | MiniMax | DOMESTIC | https://api.minimaxi.com/v1 |
| xai | xAI | INTERNATIONAL | https://api.x.ai/v1 |
| mistral | Mistral AI | INTERNATIONAL | https://api.mistral.ai/v1 |
| azure | Microsoft Azure | INTERNATIONAL | https://{resource}.openai.azure.com |
| aws_bedrock | AWS Bedrock | INTERNATIONAL | https://bedrock-runtime.{region}.amazonaws.com |
| baichuan | 百川智能 | DOMESTIC | https://api.baichuan-ai.com/v1 |

#### model-specs.json — 来自 metadata/models/*.json + 调研报告定价表

旧 models JSON 字段映射：

| 旧字段 | 新字段 | 转换 |
|---|---|---|
| provider_model_id | providerModelId | 直接映射 |
| display_name | displayName | 直接映射 |
| — | modelFamily | 从 providerModelId 推导：取第一个 - 前的部分（gpt-4o → gpt-4o, claude-sonnet-4 → claude-sonnet） |
| context_window | contextWindow | 直接映射 |
| — | maxInputTokens | 旧数据缺失，设 null |
| max_output_tokens | maxOutputTokens | 直接映射 |
| knowledge_cutoff | knowledgeCutoff | 直接映射 |
| capabilities | capabilities | 直接映射 |
| — | modalities | 从 capabilities 推导：有 vision → 加 "image"，否则只有 ["text"] |
| — | source | 固定 "BUILTIN" |
| — | syncedAt | 固定 null |
| — | state | 固定 "ACTIVE" |

#### plans.json — 来自 metadata/products/*.json + 调研报告端点/定价

旧 products JSON 字段映射：

| 旧字段 | 新字段 | 转换 |
|---|---|---|
| provider_id + product_name | planCode | 组合生成："{provider_id}_{product_name_slug}"，如 "openai_按量付费" → "openai_standard" |
| product_name | planName | 直接映射 |
| provider_id | providerCode | 直接映射 |
| product_type | billingMode | STANDARD→PAY_AS_YOU_GO, SUBSCRIPTION→SUBSCRIPTION, BATCH→PAY_AS_YOU_GO |
| endpoints | endpoints | 从 Map 转为 [{protocol: key, url: value}]，key 直接用枚举名 |
| input_price/output_price/... | pricing | 定价字段合并为 [{providerModelId, inputPrice, outputPrice, ...}]，从 product-models 关联 + 调研报告定价表获取每个模型的具体定价 |
| description | description | 直接映射 |
| — | source | 固定 "BUILTIN" |
| — | syncedAt | 固定 null |
| — | state | 固定 "ACTIVE" |

**pricing 数据合并规则**：

1. 从 product-models JSON 拿到该套餐包含的 providerModelId 列表
2. 从调研报告的模型定价表中查找每个 providerModelId 的具体价格（input/output/cache_read/reasoning 等）
3. 如果旧 products JSON 上有 input_price/output_price 但没有按模型区分，说明是套餐默认价（标注在套餐级而非模型级），需要从调研报告获取每个模型的细项价格
4. 如果调研报告中某个模型无定价数据，使用旧 products JSON 上的套餐级默认价

**新增套餐（旧 JSON 中无但调研报告中有）**：

调研报告中发现了旧 products JSON 未覆盖的套餐，需新增：

| providerCode | planCode | planName | billingMode | endpoints | 说明 |
|---|---|---|---|---|---|
| deepseek | deepseek_anthropic_compat | Anthropic兼容按量付费 | PAY_AS_YOU_GO | [{ANTHROPIC, https://api.deepseek.com/anthropic}] | DeepSeek 双协议 |
| volcengine | volcengine_coding_plan | 豆包Coding Plan | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 火山引擎双协议 |
| zhipu | zhipu_coding_lite | GLM Coding Plan Lite | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 智谱双协议 |
| zhipu | zhipu_coding_pro | GLM Coding Plan Pro | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 智谱双协议 |
| zhipu | zhipu_coding_max | GLM Coding Plan Max | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 智谱双协议 |
| qwen | qwen_coding_lite | Coding Plan Lite | SUBSCRIPTION | [{OPENAI, ...}] | 阿里云订阅 |
| qwen | qwen_coding_pro | Coding Plan Pro | SUBSCRIPTION | [{OPENAI, ...}] | 阿里云订阅 |
| wenxin | wenxin_coding_lite | 千帆Coding Plan Lite | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 百度双协议 |
| wenxin | wenxin_coding_pro | 千帆Coding Plan Pro | SUBSCRIPTION | [{OPENAI, ...}, {ANTHROPIC, ...}] | 百度双协议 |
| minimax | minimax_anthropic_compat | Anthropic兼容 | PAY_AS_YOU_GO | [{ANTHROPIC, https://api.minimaxi.com/anthropic}] | MiniMax 双协议 |
| minimax | minimax_international | 国际版按量付费 | PAY_AS_YOU_GO | [{OPENAI, https://api.minimax.io/v1}] | MiniMax 国际版 |

#### plan-models.json — 来自 metadata/product-models/*.json

旧 product-models JSON 字段映射：

| 旧字段 | 新字段 | 转换 |
|---|---|---|
| product_name → planCode | planCode | 需先查找 plans.json 中的 planCode 映射 |
| provider_model_id | providerModelId | 直接映射 |
| — | source | 固定 "BUILTIN" |
| — | syncedAt | 固定 null |
| — | state | 固定 "ACTIVE" |

**planCode 映射**：旧数据用 product_name（如"按量付费"）关联，新数据用 planCode（如"openai_standard"）。需要在生成时建立 product_name → planCode 的映射表。

### JSON 文件格式示例

#### providers.json

```json
[
  {
    "providerCode": "openai",
    "providerName": "OpenAI",
    "providerType": "INTERNATIONAL",
    "logoUrl": "https://cdn.example.com/icons/openai.png",
    "websiteUrl": "https://openai.com",
    "baseUrl": "https://api.openai.com",
    "description": "OpenAI 官方 API，支持 GPT-5.5、GPT-5.4、O3 等最新模型",
    "source": "BUILTIN",
    "syncedAt": null,
    "state": "ACTIVE"
  }
]
```

#### model-specs.json

```json
[
  {
    "providerModelId": "gpt-4o",
    "displayName": "GPT-4o",
    "modelFamily": "gpt-4o",
    "contextWindow": 128000,
    "maxInputTokens": null,
    "maxOutputTokens": 16384,
    "knowledgeCutoff": "2024-10-31",
    "capabilities": {"vision": true, "tool_use": true, "streaming": true, "prompt_caching": true},
    "modalities": ["text", "image"],
    "source": "BUILTIN",
    "syncedAt": null,
    "state": "ACTIVE"
  }
]
```

#### plans.json

```json
[
  {
    "planCode": "openai_standard",
    "providerCode": "openai",
    "planName": "按量付费",
    "billingMode": "PAY_AS_YOU_GO",
    "endpoints": [{"protocol": "OPENAI", "url": "https://api.openai.com/v1"}],
    "pricing": [
      {"providerModelId": "gpt-5.5", "inputPrice": 5.0, "outputPrice": 30.0, "cacheReadPrice": 0.5},
      {"providerModelId": "gpt-4o", "inputPrice": 2.5, "outputPrice": 10.0, "cacheReadPrice": 0.25}
    ],
    "description": "标准即时计费，支持Prompt Caching（90%折扣）",
    "source": "BUILTIN",
    "syncedAt": null,
    "state": "ACTIVE"
  },
  {
    "planCode": "deepseek_anthropic_compat",
    "providerCode": "deepseek",
    "planName": "Anthropic兼容按量付费",
    "billingMode": "PAY_AS_YOU_GO",
    "endpoints": [{"protocol": "ANTHROPIC", "url": "https://api.deepseek.com/anthropic"}],
    "pricing": [
      {"providerModelId": "deepseek-v4-pro", "inputPrice": 1.74, "outputPrice": 3.48, "cacheReadPrice": 0.0145},
      {"providerModelId": "deepseek-v4-flash", "inputPrice": 0.14, "outputPrice": 0.28, "cacheReadPrice": 0.0028}
    ],
    "description": "Anthropic协议兼容端点，支持Claude SDK直接接入",
    "source": "BUILTIN",
    "syncedAt": null,
    "state": "ACTIVE"
  }
]
```

#### plan-models.json

```json
[
  {"planCode": "openai_standard", "providerModelId": "gpt-5.5", "source": "BUILTIN", "syncedAt": null, "state": "ACTIVE"},
  {"planCode": "openai_standard", "providerModelId": "gpt-4o", "source": "BUILTIN", "syncedAt": null, "state": "ACTIVE"},
  {"planCode": "deepseek_anthropic_compat", "providerModelId": "deepseek-v4-pro", "source": "BUILTIN", "syncedAt": null, "state": "ACTIVE"}
]
```

## 最终包结构

```
domain/supply/catalog/
  entity/
    ProviderCatalog.java
    PlanCatalog.java
    PlanModelCatalog.java
    ModelSpecCatalog.java
  enums/
    CatalogSource.java
    CatalogState.java
  gateway/
    ProviderCatalogGateway.java
    PlanCatalogGateway.java
    PlanModelCatalogGateway.java
    ModelSpecCatalogGateway.java
  service/
    CatalogDomainService.java

infrastructure/supply/catalog/
  gateway/
    ProviderCatalogGatewayImpl.java
    PlanCatalogGatewayImpl.java
    PlanModelCatalogGatewayImpl.java
    ModelSpecCatalogGatewayImpl.java
  database/
    ProviderCatalogDo.java
    PlanCatalogDo.java
    PlanModelCatalogDo.java
    ModelSpecCatalogDo.java
    ProviderCatalogRepository.java
    PlanCatalogRepository.java
    PlanModelCatalogRepository.java
    ModelSpecCatalogRepository.java
  loader/
    BuiltinCatalogLoader.java
  sync/
    ModelsDevSyncClient.java
    CatalogSyncScheduler.java

application/catalog/
  CatalogService.java
  CatalogServiceImpl.java
  CatalogSyncService.java
  CatalogMaterializeService.java
  dto/
    CatalogSyncResult.java
    PlanDetailResponse.java
    MaterializeRequest.java
    MaterializeResult.java
    ...

adapter/api/
  CatalogController.java
```