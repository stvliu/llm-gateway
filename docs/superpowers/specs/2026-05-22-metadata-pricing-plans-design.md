# 元数据体系优化设计文档

**设计日期**: 2026-05-22
**设计版本**: v2.0
**状态**: 待实现

---

## 一、背景与目标

### 1.1 背景

当前系统元数据结构：
- `provider_metadata`: 供应商元数据
- `model_metadata`: 模型元数据（直接关联 `provider_id`）

**存在的问题**：
1. 缺少中间层"产品"，无法表达同一供应商的不同产品/套餐
2. 无法表达多端点配置（如 DeepSeek 同时支持 OpenAI 和 Anthropic 协议）
3. 模型直接关联供应商，无法区分不同产品下的模型
4. 模型信息过时，缺少最新模型和价格

### 1.2 目标

建立三级元数据关系：

```
ProviderMetadata → ProductMetadata → ModelMetadata
     供应商            产品/套餐           模型
```

核心目标：
1. 新增 `product_metadata` 表，管理产品/套餐和多协议端点
2. 调整 `model_metadata` 关联关系，改为关联 `product_id`
3. 更新现有 13 家厂商的模型元数据为最新版本
4. 保持原币种价格（国际厂商美元，国内厂商人民币）
5. 支持自动同步机制（启动时从 JSON 文件同步到数据库）

---

## 二、数据模型设计

### 2.1 ER 关系图

```
┌─────────────────────┐
│  provider_metadata  │
│─────────────────────│
│ provider_id (PK)    │
│ provider_name       │
│ provider_type       │
│ description         │
│ icon_url            │
│ website_url         │
│ api_doc_url         │
│ tags                │
└─────────┬───────────┘
          │ 1
          │
          │ N
┌─────────▼───────────┐
│  product_metadata   │
│─────────────────────│
│ id (PK)             │
│ provider_id (FK)    │
│ product_name        │
│ product_type        │
│ endpoints (JSON)    │◄── 多协议端点 {"OPENAI": "url", "ANTHROPIC": "url"}
│ is_default          │
│ description         │
└─────────┬───────────┘
          │ 1
          │
          │ N
┌─────────▼───────────┐
│   model_metadata    │
│─────────────────────│
│ id (PK)             │
│ product_id (FK)     │◄── 改为关联产品
│ provider_model_id   │
│ display_name        │
│ input_price         │
│ output_price        │
│ capabilities        │
└─────────────────────┘
```

### 2.2 provider_metadata 表（保持）

供应商基本信息，不包含端点配置。

```sql
CREATE TABLE provider_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL UNIQUE,
    provider_name VARCHAR(128) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    description TEXT,
    icon_url VARCHAR(512),
    website_url VARCHAR(512),
    api_doc_url VARCHAR(512),
    tags JSON,
    state VARCHAR(32) DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);
```

### 2.3 product_metadata 表（新增）

产品/套餐元数据，包含多协议端点配置。

```sql
CREATE TABLE product_metadata (
    id BIGSERIAL PRIMARY KEY,

    -- 关联供应商
    provider_id VARCHAR(64) NOT NULL,

    -- 产品基本信息
    product_name VARCHAR(128) NOT NULL,
    product_type VARCHAR(32) NOT NULL,     -- STANDARD/BATCH/CACHE/SUBSCRIPTION/...
    description TEXT,

    -- 多协议端点配置
    endpoints JSON NOT NULL,               -- {"OPENAI": "url", "ANTHROPIC": "url"}

    -- 元数据
    is_default BOOLEAN DEFAULT false,
    state VARCHAR(32) DEFAULT 'ACTIVE',
    source VARCHAR(32) DEFAULT 'BUILTIN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 索引
CREATE UNIQUE INDEX uk_product_metadata_provider_name
    ON product_metadata(provider_id, product_name);
CREATE INDEX idx_product_metadata_provider ON product_metadata(provider_id);
CREATE INDEX idx_product_metadata_type ON product_metadata(product_type);
```

### 2.4 model_metadata 表（调整关联关系）

模型元数据，改为关联 `product_id`。

```sql
CREATE TABLE model_metadata (
    id BIGSERIAL PRIMARY KEY,

    -- 关联产品（调整）
    product_id BIGINT NOT NULL,

    -- 模型基本信息
    provider_model_id VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    model_family VARCHAR(64),
    context_window INTEGER,
    max_input_tokens INTEGER,
    max_output_tokens INTEGER,

    -- 价格
    input_price DECIMAL(12,6),
    output_price DECIMAL(12,6),
    reasoning_price DECIMAL(12,6),
    cache_read_price DECIMAL(12,6),
    cache_write_price DECIMAL(12,6),

    -- 能力
    capabilities JSON,
    modalities JSON,

    -- 元数据
    knowledge_cutoff VARCHAR(32),
    release_date DATE,
    open_weights BOOLEAN,
    state VARCHAR(32) DEFAULT 'ACTIVE',
    source VARCHAR(32) DEFAULT 'BUILTIN',
    source_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 索引（调整）
CREATE UNIQUE INDEX uk_model_metadata_product_model
    ON model_metadata(product_id, provider_model_id);
CREATE INDEX idx_model_metadata_product ON model_metadata(product_id);
```

---

## 三、枚举定义

### 3.1 ProductType（产品类型）

```java
public enum ProductType {
    STANDARD,       // 标准按量付费
    BATCH,          // 批量异步
    CACHE,          // 缓存折扣
    SUBSCRIPTION,   // 订阅制（Coding Plan、Token Plan）
    PROMOTION,      // 限时优惠
    FREE_TIER       // 免费额度
}
```

### 3.2 Protocol（协议类型）

```java
public enum Protocol {
    OPENAI,         // OpenAI 原生/兼容协议
    ANTHROPIC,      // Anthropic Messages API
    GEMINI,         // Google Gemini API
    NATIVE          // 原生私有协议
}
```

---

## 四、JSON 文件结构

### 4.1 目录结构

```
src/main/resources/metadata/
├── providers/
│   ├── anthropic.json
│   ├── deepseek.json
│   ├── gemini.json
│   ├── minimax.json
│   ├── moonshot.json
│   ├── openai.json
│   ├── qwen.json
│   ├── tencent.json
│   ├── volcengine.json
│   ├── wenxin.json
│   ├── xunfei.json
│   ├── zhipu.json
│   └── baichuan.json
├── products/              ← 新增目录
│   ├── anthropic.json
│   ├── deepseek.json
│   ├── gemini.json
│   ├── minimax.json
│   ├── moonshot.json
│   ├── openai.json
│   ├── qwen.json
│   ├── tencent.json
│   ├── volcengine.json
│   ├── wenxin.json
│   ├── xunfei.json
│   ├── zhipu.json
│   └── baichuan.json
└── models/
    ├── anthropic.json
    ├── deepseek.json
    └── ... (同上，结构调整为关联 product_id)
```

### 4.2 providers/*.json 格式

```json
{
  "provider_id": "deepseek",
  "provider_name": "DeepSeek",
  "provider_type": "DEEPSEEK",
  "description": "DeepSeek API，高性价比国产大模型",
  "icon_url": "https://cdn.example.com/icons/deepseek.png",
  "website_url": "https://deepseek.com",
  "api_doc_url": "https://api-docs.deepseek.com",
  "tags": ["国内", "高性价比", "代码", "推理"]
}
```

### 4.3 products/*.json 格式（新增）

```json
[
  {
    "provider_id": "deepseek",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.deepseek.com",
      "ANTHROPIC": "https://api.deepseek.com/anthropic"
    },
    "is_default": true,
    "description": "标准按量计费，支持双协议"
  },
  {
    "provider_id": "deepseek",
    "product_name": "缓存折扣",
    "product_type": "CACHE",
    "endpoints": {
      "OPENAI": "https://api.deepseek.com"
    },
    "description": "缓存命中折扣，输入价格低至2.5%"
  }
]
```

### 4.4 models/*.json 格式（调整）

```json
[
  {
    "product_name": "按量付费",
    "provider_model_id": "deepseek-v4-pro",
    "display_name": "DeepSeek V4 Pro",
    "context_window": 1048576,
    "max_output_tokens": 384000,
    "input_price": 3.0,
    "output_price": 6.0,
    "capabilities": {
      "vision": false,
      "function_calling": true,
      "streaming": true
    }
  },
  {
    "product_name": "按量付费",
    "provider_model_id": "deepseek-v4-flash",
    "display_name": "DeepSeek V4 Flash",
    "context_window": 1048576,
    "max_output_tokens": 384000,
    "input_price": 1.0,
    "output_price": 2.0,
    "capabilities": {
      "vision": false,
      "function_calling": true,
      "streaming": true
    }
  }
]
```

> **注意**: models/*.json 中使用 `product_name` 而非 `product_id`，
> 同步时通过 `(provider_id, product_name)` 查找对应的 product_id。

---

## 五、产品元数据示例

### 5.1 DeepSeek

```json
[
  {
    "provider_id": "deepseek",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.deepseek.com",
      "ANTHROPIC": "https://api.deepseek.com/anthropic"
    },
    "is_default": true
  }
]
```

### 5.2 智谱AI

```json
[
  {
    "provider_id": "zhipu",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/paas/v4/"
    },
    "is_default": true
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Lite",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    }
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Pro",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    }
  },
  {
    "provider_id": "zhipu",
    "product_name": "Coding Plan Max",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://open.bigmodel.cn/api/coding/paas/v4"
    }
  }
]
```

### 5.3 火山方舟

```json
[
  {
    "provider_id": "volcengine",
    "product_name": "在线推理",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://ark.cn-beijing.volces.com/api/v3"
    },
    "is_default": true
  },
  {
    "provider_id": "volcengine",
    "product_name": "Coding Plan",
    "product_type": "SUBSCRIPTION",
    "endpoints": {
      "OPENAI": "https://ark.cn-beijing.volces.com/api/coding/v3",
      "ANTHROPIC": "https://ark.cn-beijing.volces.com/api/coding"
    }
  }
]
```

### 5.4 阿里云（多区域）

```json
[
  {
    "provider_id": "qwen",
    "product_name": "按量付费-北京",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope.aliyuncs.com/compatible-mode/v1"
    },
    "is_default": true
  },
  {
    "provider_id": "qwen",
    "product_name": "按量付费-弗吉尼亚",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope-us.aliyuncs.com/compatible-mode/v1"
    }
  },
  {
    "provider_id": "qwen",
    "product_name": "按量付费-新加坡",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"
    }
  }
]
```

### 5.5 MiniMax

```json
[
  {
    "provider_id": "minimax",
    "product_name": "按量付费",
    "product_type": "STANDARD",
    "endpoints": {
      "OPENAI": "https://api.minimaxi.com/v1",
      "ANTHROPIC": "https://api.minimaxi.com/anthropic"
    },
    "is_default": true
  }
]
```

---

## 六、代码结构设计

### 6.1 领域层新增

```
domain/metadata/
├── entity/
│   ├── ProviderMetadata.java      -- 已存在
│   ├── ProductMetadata.java       -- 新增
│   └── ModelMetadata.java         -- 已存在（调整关联）
├── gateway/
│   ├── ProviderMetadataGateway.java
│   ├── ProductMetadataGateway.java    -- 新增
│   └── ModelMetadataGateway.java
├── service/
│   ├── ProviderMetadataDomainService.java
│   ├── ProductMetadataDomainService.java  -- 新增
│   └── ModelMetadataDomainService.java
└── enums/
    ├── ProductType.java               -- 新增
    └── Protocol.java                  -- 新增
```

### 6.2 基础设施层新增

```
infrastructure/metadata/
├── database/
│   ├── ProviderMetadataRepository.java
│   ├── ProductMetadataRepository.java   -- 新增
│   ├── ModelMetadataRepository.java
│   ├── ProviderMetadataDo.java
│   ├── ProductMetadataDo.java           -- 新增
│   └── ModelMetadataDo.java
├── gateway/
│   ├── ProviderMetadataGatewayImpl.java
│   ├── ProductMetadataGatewayImpl.java  -- 新增
│   └── ModelMetadataGatewayImpl.java
└── repository/
    └── BuiltinMetadataLoader.java       -- 扩展，支持加载 products
```

### 6.3 应用层新增

```
application/metadata/
├── ProviderMetadataService.java
├── ProductMetadataService.java      -- 新增
├── ModelMetadataService.java
└── dto/
    ├── ProviderMetadataResponse.java
    ├── ProductMetadataResponse.java     -- 新增
    └── ModelMetadataResponse.java
```

### 6.4 同步机制扩展

扩展 `BuiltinMetadataSyncRunner`，支持 product_metadata 同步：

```java
@Component
public class BuiltinMetadataSyncRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // 1. 同步供应商元数据
        syncProviders();

        // 2. 同步产品元数据（新增）
        syncProducts();

        // 3. 同步模型元数据
        syncModels();
    }

    private void syncProducts() {
        List<ProductMetadata> products = metadataLoader.loadProducts();
        for (ProductMetadata product : products) {
            productMetadataGateway.saveOrUpdate(product);
        }
    }
}
```

---

## 七、模型元数据更新清单

根据文档调研，更新以下供应商的模型信息：

| 供应商 | 当前模型（删除） | 更新为（新增） |
|--------|-----------------|---------------|
| **Anthropic** | Claude Opus 4, Sonnet 4, 3.5 Haiku | Opus 4.7, Sonnet 4.6, Haiku 4.5 |
| **OpenAI** | GPT-4.1, GPT-4o, O3, O4-mini | GPT-5.5, GPT-5.4, GPT-5.4 mini, O3, O3-Pro |
| **DeepSeek** | Chat, Coder, Reasoner | V4-Pro, V4-Flash, V3.2, R1 |
| **Gemini** | 2.5 Pro/Flash, 2.0 Flash, 1.5 | 3.1 Pro, 3.1 Flash-Lite, 3 Flash, 2.5 Flash-Lite |
| **Qwen** | Max, Plus, Turbo, VL | Qwen3-Max, Qwen3.5-Plus, Qwen3-Turbo, Qwen3-VL, Qwen3-Coder |
| **Zhipu** | GLM-4 Plus/Air/Flash | GLM-5.1, GLM-5, GLM-5-Turbo, GLM-4.7 |
| **Moonshot** | Kimi 8K/32K/128K | kimi-k2.6, kimi-k2.5, moonshot-v1-vision |
| **MiniMax** | (需确认) | M2.7, M2.5, M2.1, Hailuo-2.3, Speech-2.8 |
| **Volcengine** | (需确认) | Doubao-Seed-2.0 系列 |
| **Wenxin** | (需确认) | ERNIE-5.1, ERNIE-5.0, ERNIE-X1.1, ERNIE-4.5-Turbo |
| **Xunfei** | (需确认) | X2-Flash, X2, X1.5, Ultra, Max, Pro |
| **Tencent** | (需确认) | Hunyuan-T1, TurboS, a13b, Vision 1.5 |

---

## 八、实现步骤

### Phase 1: 数据库迁移
1. 创建 V13 迁移脚本
2. 新增 `product_metadata` 表
3. 调整 `model_metadata` 表（新增 `product_id`，保留 `provider_id` 用于过渡）

### Phase 2: 领域层实现
1. 新增 `ProductMetadata` 实体
2. 新增 `ProductType`, `Protocol` 枚举
3. 新增 `ProductMetadataGateway` 接口
4. 新增 `ProductMetadataDomainService`

### Phase 3: 基础设施层实现
1. 新增 `ProductMetadataDo` 数据库对象
2. 新增 `ProductMetadataRepository`
3. 新增 `ProductMetadataGatewayImpl`
4. 扩展 `BuiltinMetadataLoader` 支持加载 products

### Phase 4: 应用层实现
1. 新增 `ProductMetadataService`
2. 新增 DTO 类

### Phase 5: JSON 文件更新
1. 新增所有 `products/*.json`
2. 更新所有 `models/*.json`（关联 product_name）

### Phase 6: 同步机制扩展
1. 扩展 `BuiltinMetadataSyncRunner` 支持 product 同步

### Phase 7: 测试
1. 单元测试
2. 集成测试
3. 启动验证同步机制

---

## 九、API 设计

### 9.1 查询供应商的产品列表

```
GET /api/v1/metadata/providers/{providerId}/products
```

响应：
```json
{
  "provider_id": "deepseek",
  "products": [
    {
      "product_name": "按量付费",
      "product_type": "STANDARD",
      "endpoints": {
        "OPENAI": "https://api.deepseek.com",
        "ANTHROPIC": "https://api.deepseek.com/anthropic"
      },
      "is_default": true
    }
  ]
}
```

### 9.2 查询产品的模型列表

```
GET /api/v1/metadata/products/{productId}/models
```

### 9.3 查询产品支持的协议

```
GET /api/v1/metadata/products/{productId}/protocols
```

响应：
```json
{
  "product_id": 1,
  "product_name": "按量付费",
  "protocols": ["OPENAI", "ANTHROPIC"]
}
```

---

## 十、风险与注意事项

1. **数据迁移**：现有 `model_metadata.provider_id` 需迁移到 `product_id`
2. **向后兼容**：保留 `model_metadata.provider_id` 字段用于过渡期
3. **JSON 同步**：models/*.json 使用 `product_name` 而非 `product_id`，需在同步时解析
4. **默认产品**：每个供应商需有一个 `is_default=true` 的产品

---

## 十一、验收标准

1. 数据库迁移成功，`product_metadata` 表创建完成
2. `model_metadata` 表新增 `product_id` 字段
3. 启动应用，自动同步所有 JSON 文件到数据库
4. API 可查询供应商的产品列表
5. API 可查询产品的模型列表
6. API 可查询产品支持的协议类型
7. 所有 13 家厂商的模型信息更新为最新版本

---

**设计完成，待用户审核后进入实现阶段。**