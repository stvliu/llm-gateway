# ProductModel 关联表重构设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将定价从模型移到产品上，新建纯关系表承载产品-模型 M:N 关联，使模型成为纯粹的属性实体。

**Architecture:** 元数据体系和业务体系同步重构。ProductMetadata/Product 新增定价字段、移除 models 列表；新建 ProductModelMetadata/ProductModel 纯关系表；ModelMetadata/Model 移除定价字段。两套体系结构完全对齐。

**Tech Stack:** Java 21, Spring Boot 3.5.x, JPA, PostgreSQL, Flyway, React, Ant Design, TanStack Query

---

## 1. 问题

当前 `model_metadata` 表将模型固有属性（context_window、capabilities）和定价字段（input_price 等 7 个）混在一条记录中，且通过 `product_id` 列建立与产品的 1:N 关系。这导致两个问题：

1. **同一模型无法属于多个产品**——DeepSeek V4 Flash 同时出现在深度求索"按量付费"和火山方舟"在线推理"中，但 1:N 外键只能指向一个产品
2. **定价语义错位**——定价是产品的责任（产品决定自己的定价方案），但定价数据挂在模型上

业务体系同样存在此问题：`models` 表有 `input_price`/`output_price`，`products` 表有 `models` JSON 列表。

## 2. 核心原则

**定价是产品的责任。产品决定自己的统一定价方案，产品决定自己包含哪些模型。模型本身没有定价。**

## 3. 目标数据模型

### 3.1 元数据体系

```
ProviderMetadata ──1:N──→ ProductMetadata
                              │ 持有定价
                              │ 1:N（子集合）
                              ▼
                         ProductModelMetadata（纯关系）
                              │ 引用
                              ▼
                         ModelMetadata（纯模型属性）
```

**ProductMetadata 变更：**
- 新增 7 个定价字段：inputPrice, outputPrice, reasoningPrice, cacheReadPrice, cacheWritePrice, inputAudioPrice, outputAudioPrice
- 其余字段不变

**ProductModelMetadata 新建：**
- id, productId, modelId
- createdAt, createdBy, updatedAt, updatedBy
- 唯一索引：(product_id, model_id)
- **无定价字段**

**ModelMetadata 变更：**
- 移除：productId, inputPrice, outputPrice, reasoningPrice, cacheReadPrice, cacheWritePrice, inputAudioPrice, outputAudioPrice
- 保留：id, providerId, providerModelId, displayName, modelFamily, contextWindow, maxInputTokens, maxOutputTokens, knowledgeCutoff, releaseDate, openWeights, modalities, capabilities, source, sourceSyncedAt, state, createdAt, createdBy, updatedAt, updatedBy

### 3.2 业务体系

```
Provider ──1:N──→ Product
                     │ 持有定价
                     │ 1:N（子集合）
                     ▼
                ProductModel（纯关系）
                     │ 引用
                     ▼
                Model（纯模型属性）
```

**Product 变更：**
- 新增：inputPrice, outputPrice
- 移除：models (List\<String\>)
- 其余字段不变

**ProductModel 新建：**
- id, productId, modelId
- createdAt, updatedAt
- 唯一索引：(product_id, model_id)
- **无定价字段**

**Model 变更：**
- 移除：inputPrice, outputPrice
- 保留：id, providerId, providerName, providerModelId, displayName, contextWindow, capabilities, state, priority, weight, createdAt, createdBy, updatedAt, updatedBy

### 3.3 两套体系对齐

| 元数据体系 | 业务体系 | 职责 |
|-----------|---------|------|
| ProviderMetadata | Provider | 供应商 |
| ProductMetadata（持有定价） | Product（持有定价） | 产品 = 定价单位 |
| ProductModelMetadata（纯关系） | ProductModel（纯关系） | 产品包含哪些模型 |
| ModelMetadata（纯属性） | Model（纯属性） | 模型属性 |

## 4. API 变更

### 4.1 元数据 API

**ProductMetadata API 新增定价字段：**
- `GET /api/v1/product-metadata` — 响应新增 7 个定价字段
- `POST /api/v1/product-metadata` — 请求支持定价字段
- `PUT /api/v1/product-metadata/{id}` — 支持更新定价字段

**ProductModelMetadata API 新建：**
- `GET /api/v1/product-model-metadata/products/{productId}` — 查某产品包含的所有模型
- `GET /api/v1/product-model-metadata/models/{modelId}` — 查某模型属于哪些产品
- `POST /api/v1/product-model-metadata` — 创建产品-模型关联（请求体：productId, modelId）
- `DELETE /api/v1/product-model-metadata/{id}` — 删除关联

**ModelMetadata API 变更：**
- `GET /api/v1/model-metadata` — 响应移除 7 个定价字段 + productId
- `POST /api/v1/model-metadata` — 请求移除定价字段
- `PUT /api/v1/model-metadata/{id}` — 移除定价字段
- **移除** `GET /api/v1/model-metadata/products/{productId}` — 改由 ProductModelMetadata API 提供

### 4.2 业务 API

**Model API 变更：**
- 响应移除 inputPrice, outputPrice

**Product API 变更：**
- 响应新增 inputPrice, outputPrice
- 响应移除 models 列表（改由 ProductModel 子资源提供）

**ProductModel API 新建：**
- `GET /api/v1/products/{productId}/models` — 查某产品包含的所有模型
- `POST /api/v1/products/{productId}/models` — 添加模型到产品（请求体：modelId）
- `DELETE /api/v1/products/{productId}/models/{modelId}` — 从产品移除模型

## 5. 同步逻辑变更

### 5.1 MetadataSyncService

当前同步流程：providers → products → models，模型创建时设置定价字段。

重构后：
1. 同步 providers（不变）
2. 同步 products：从模型 JSON 中提取定价，写入产品元数据的定价字段
3. 同步 models：只设置模型固有属性，不设置定价
4. 新增同步 step：同步 product-model-metadata 关联记录

### 5.2 ProviderMetadataService.apply()

当前逻辑：创建 Provider → Product → ProductApiKey → Model，创建 Model 时设置 inputPrice/outputPrice。

重构后：
1. 创建 Provider（不变）
2. 创建 Product：从 ProductMetadata 获取定价，设置 inputPrice/outputPrice
3. 创建 ProductApiKey（不变）
4. 创建 Model：不再设置定价
5. 新增：创建 ProductModel 关联记录（产品包含哪些模型）

### 5.3 ProductRoutingService

当前逻辑：`productDomainService.containsModel(product, modelName)` 通过 Product.models 列表匹配。

重构后：改为查询 ProductModelGateway 确定产品是否包含指定模型。

## 6. JSON 数据文件变更

### 6.1 models/*.json（12 个文件）

当前格式：
```json
[{"product_name": "按量付费", "provider_model_id": "deepseek-v4-pro", "input_price": 3.00, "output_price": 6.00, "cache_read_price": 0.025, ...}]
```

重构后——只保留模型固有属性：
```json
[{"provider_model_id": "deepseek-v4-pro", "display_name": "DeepSeek V4 Pro", "context_window": 1048576, "capabilities": {"vision": false, "function_calling": true, "streaming": true, "reasoning": true}, "knowledge_cutoff": "2025-04-15"}]
```

移除字段：product_name, input_price, output_price, reasoning_price, cache_read_price, cache_write_price, input_audio_price, output_audio_price

### 6.2 products/*.json

当前格式（无定价）：
```json
[{"provider_id": "deepseek", "product_name": "按量付费", "product_type": "STANDARD", "endpoints": {"OPENAI": "https://api.deepseek.com/v1"}, "is_default": true}]
```

重构后——新增定价字段：
```json
[{"provider_id": "deepseek", "product_name": "按量付费", "product_type": "STANDARD", "endpoints": {"OPENAI": "https://api.deepseek.com/v1"}, "is_default": true, "input_price": 0.14, "output_price": 0.28, "cache_read_price": 0.0028}]
```

### 6.3 新建 product-models/*.json（12 个文件）

每个供应商一个文件，定义产品包含哪些模型：
```json
[
  {"product_name": "按量付费", "provider_model_id": "deepseek-v4-pro"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-v4-flash"},
  {"product_name": "按量付费", "provider_model_id": "deepseek-r1"}
]
```

## 7. 数据迁移策略

### 7.1 元数据体系 (V34)

1. 创建 `product_model_metadata` 纯关系表
2. `product_metadata` 表新增 7 个定价列
3. 从 `model_metadata` 迁移定价到 `product_metadata`（每个供应商的默认产品取第一个有定价模型的价格）
4. 从 `model_metadata.product_id` 迁移关系到 `product_model_metadata`
5. 从 `model_metadata` 删除定价列和 `product_id` 列

### 7.2 业务体系 (V35)

1. 创建 `product_models` 纯关系表
2. `products` 表新增 input_price, output_price 列，删除 models 列
3. 从 `models` 迁移定价到 `products`
4. 从 `models.product_id` 迁移关系到 `product_models`
5. 从 `models` 删除定价列和 `product_id` 列

## 8. 测试验证

1. Flyway 迁移执行成功，数据完整性校验
2. 元数据同步：产品持有定价，模型无定价，关联表正确
3. Apply 流程：创建的业务 Product 持有定价，Model 无定价，ProductModel 正确
4. 路由逻辑：通过 ProductModel 关联匹配模型
5. 前端：产品展示定价，模型不展示定价，三级导航正常

## 9. 不受影响的领域

- **ProxyServiceImpl / ChannelRoutingService** — 路由层通过 ProductRoutingService 间接使用
- **domain/security / domain/quota / domain/audit** — 不涉及定价
- **domain/alert** — 不涉及定价