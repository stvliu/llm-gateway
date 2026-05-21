# 元数据优化设计文档 - Pricing Plans 与多端点支持

**设计日期**: 2026-05-22
**设计版本**: v1.0
**状态**: 待实现

---

## 一、背景与目标

### 1.1 背景

当前系统元数据结构：
- `provider_metadata`: 供应商基本信息，包含单一 `base_url`
- `model_metadata`: 模型信息，包含基础价格

**存在的问题**：
1. 无法表达多端点配置（如 DeepSeek 同时支持 OpenAI 和 Anthropic 协议）
2. 无法管理产品/套餐信息（如 Coding Plan、批量折扣、缓存折扣）
3. 无法表达速率限制层级（如 Kimi 的 Tier 0-5 分层）
4. 模型信息过时，缺少最新模型和价格

### 1.2 目标

基于文档调研结果，完善元数据体系：
1. 新增 `pricing_plans` 表，管理产品/套餐、端点配置、协议兼容性
2. 更新现有 13 家厂商的模型元数据为最新版本
3. 保持原币种价格（国际厂商美元，国内厂商人民币）
4. 删除旧版本模型，只保留文档中的最新模型
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
│   pricing_plans     │
│─────────────────────│
│ id (PK)             │
│ provider_id (FK)    │
│ plan_type           │
│ plan_name           │
│ base_url            │◄── 端点配置在此
│ protocol            │◄── 协议类型在此
│ region              │
│ input_price_mult    │
│ subscription_fee    │
│ concurrent_limit    │◄── 速率限制在此
│ rpm_limit           │
│ tpm_limit           │
└─────────┬───────────┘
          │
          │ N
┌─────────▼───────────┐
│   model_metadata    │
│─────────────────────│
│ id (PK)             │
│ provider_id (FK)    │
│ provider_model_id   │
│ display_name        │
│ input_price         │◄── 模型独立价格
│ output_price        │
│ capabilities        │
└─────────────────────┘
```

### 2.2 provider_metadata 表（简化）

移除 `provider_config` 中的 `base_url`，端点配置移至 `pricing_plans`。

```sql
-- provider_metadata 表结构（调整后）
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

### 2.3 pricing_plans 表（新增）

核心表，包含端点配置、协议、价格策略、速率限制。

```sql
CREATE TABLE pricing_plans (
    id BIGSERIAL PRIMARY KEY,

    -- 关联供应商
    provider_id VARCHAR(64) NOT NULL,

    -- 套餐基本信息
    plan_type VARCHAR(32) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    description TEXT,

    -- 端点配置（套餐级别）
    base_url VARCHAR(512) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    region VARCHAR(32),

    -- 价格配置
    input_price_multiplier DECIMAL(6,4) DEFAULT 1.0,
    output_price_multiplier DECIMAL(6,4) DEFAULT 1.0,
    cache_input_price DECIMAL(12,6),

    -- 订阅制
    subscription_fee DECIMAL(12,2),
    subscription_currency VARCHAR(8),
    subscription_period VARCHAR(16),
    request_limit INT,
    token_limit BIGINT,

    -- 速率限制
    concurrent_limit INT,
    rpm_limit INT,
    tpm_limit BIGINT,

    -- 促销
    valid_from DATE,
    valid_until DATE,

    -- 元数据
    is_default BOOLEAN DEFAULT false,
    priority INT DEFAULT 0,
    state VARCHAR(32) DEFAULT 'ACTIVE',
    source VARCHAR(32) DEFAULT 'BUILTIN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

-- 索引
CREATE UNIQUE INDEX uk_pricing_plans_provider_plan
    ON pricing_plans(provider_id, plan_type, plan_name);
CREATE INDEX idx_pricing_plans_provider ON pricing_plans(provider_id);
CREATE INDEX idx_pricing_plans_type ON pricing_plans(plan_type);
CREATE INDEX idx_pricing_plans_protocol ON pricing_plans(protocol);
CREATE INDEX idx_pricing_plans_region ON pricing_plans(region);
```

### 2.4 model_metadata 表（保持现有）

模型价格独立存储，可参考套餐价格倍率计算实际价格。

```sql
-- model_metadata 表结构（保持现有）
CREATE TABLE model_metadata (
    id BIGSERIAL PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL,
    provider_model_id VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    model_family VARCHAR(64),
    context_window INTEGER,
    max_input_tokens INTEGER,
    max_output_tokens INTEGER,
    input_price DECIMAL(12,6),
    output_price DECIMAL(12,6),
    reasoning_price DECIMAL(12,6),
    cache_read_price DECIMAL(12,6),
    cache_write_price DECIMAL(12,6),
    input_audio_price DECIMAL(12,6),
    output_audio_price DECIMAL(12,6),
    knowledge_cutoff VARCHAR(32),
    release_date DATE,
    open_weights BOOLEAN,
    modalities JSON,
    capabilities JSON,
    source VARCHAR(32) DEFAULT 'BUILTIN',
    source_synced_at TIMESTAMP WITH TIME ZONE,
    state VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by BIGINT
);

CREATE UNIQUE INDEX uk_model_metadata_provider_model
    ON model_metadata(provider_id, provider_model_id);
```

---

## 三、枚举定义

### 3.1 PlanType（套餐类型）

```java
public enum PlanType {
    STANDARD,       // 标准按量付费
    BATCH,          // 批量异步（50%折扣）
    CACHE,          // 缓存命中折扣
    SUBSCRIPTION,   // 月度订阅制
    PROVISIONED,    // 预留容量/吞吐量
    FLEX,           // 灵活调度折扣
    PRIORITY,       // 优先保障溢价
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
    NATIVE,         // 原生私有协议
    OPENAI_COMPAT   // OpenAI 兼容（第三方）
}
```

### 3.3 Region（区域）

```java
public enum Region {
    CN,             // 中国大陆
    US,             // 美国
    SG,             // 新加坡
    EU,             // 欧洲
    GLOBAL          // 全球
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
├── models/
│   ├── anthropic.json
│   ├── deepseek.json
│   └── ... (同上)
└── pricing/          ← 新增目录
    ├── anthropic.json
    ├── deepseek.json
    ├── gemini.json
    ├── minimax.json
    ├── moonshot.json
    ├── openai.json
    ├── qwen.json
    ├── tencent.json
    ├── volcengine.json
    ├── wenxin.json
    ├── xunfei.json
    ├── zhipu.json
    └── baichuan.json
```

### 4.2 providers/*.json 格式（简化）

```json
{
  "provider_id": "deepseek",
  "provider_name": "DeepSeek",
  "provider_type": "DEEPSEEK",
  "description": "DeepSeek API，高性价比国产大模型，支持对话、代码生成和深度推理",
  "icon_url": "https://cdn.example.com/icons/deepseek.png",
  "website_url": "https://deepseek.com",
  "api_doc_url": "https://api-docs.deepseek.com",
  "tags": ["国内", "高性价比", "代码", "推理"]
}
```

### 4.3 pricing/*.json 格式（新增）

```json
[
  {
    "provider_id": "deepseek",
    "plan_type": "STANDARD",
    "plan_name": "按量付费",
    "base_url": "https://api.deepseek.com",
    "protocol": "OPENAI",
    "is_default": true,
    "description": "OpenAI兼容端点，标准按Token计费"
  },
  {
    "provider_id": "deepseek",
    "plan_type": "STANDARD",
    "plan_name": "按量付费",
    "base_url": "https://api.deepseek.com/anthropic",
    "protocol": "ANTHROPIC",
    "is_default": false,
    "description": "Anthropic兼容端点，支持Claude SDK"
  },
  {
    "provider_id": "deepseek",
    "plan_type": "CACHE",
    "plan_name": "缓存命中",
    "base_url": "https://api.deepseek.com",
    "protocol": "OPENAI",
    "cache_input_price": 0.025,
    "input_price_multiplier": 0.025,
    "description": "输入缓存命中价格低至原价2.5%"
  },
  {
    "provider_id": "deepseek",
    "plan_type": "PROMOTION",
    "plan_name": "V4-Pro限时优惠",
    "base_url": "https://api.deepseek.com",
    "protocol": "OPENAI",
    "input_price_multiplier": 0.25,
    "valid_until": "2026-05-31",
    "description": "输入价格2.5折优惠至2026/05/31"
  }
]
```

### 4.4 models/*.json 格式（更新）

```json
[
  {
    "provider_id": "deepseek",
    "provider_model_id": "deepseek-v4-pro",
    "display_name": "DeepSeek V4 Pro",
    "context_window": 1048576,
    "max_output_tokens": 384000,
    "input_price": 3.0,
    "output_price": 6.0,
    "source": "BUILTIN",
    "capabilities": {
      "vision": false,
      "function_calling": true,
      "streaming": true
    }
  },
  {
    "provider_id": "deepseek",
    "provider_model_id": "deepseek-v4-flash",
    "display_name": "DeepSeek V4 Flash",
    "context_window": 1048576,
    "max_output_tokens": 384000,
    "input_price": 1.0,
    "output_price": 2.0,
    "source": "BUILTIN",
    "capabilities": {
      "vision": false,
      "function_calling": true,
      "streaming": true
    }
  }
]
```

---

## 五、代码结构设计

### 5.1 领域层新增

```
domain/pricing/
├── entity/
│   └── PricingPlan.java              -- 定价计划实体
├── gateway/
│   └── PricingPlanGateway.java       -- Gateway接口
├── service/
│   └── PricingPlanDomainService.java -- 领域服务
└── enums/
    ├── PlanType.java                  -- 套餐类型枚举
    ├── Protocol.java                  -- 协议类型枚举
    └── Region.java                    -- 区域枚举
```

### 5.2 基础设施层新增

```
infrastructure/pricing/
├── database/
│   ├── PricingPlanRepository.java    -- JPA Repository
│   └── PricingPlanDo.java            -- 数据库对象
├── gateway/
│   └── PricingPlanGatewayImpl.java   -- Gateway实现
└── repository/
    └── BuiltinPricingLoader.java     -- JSON加载器
```

### 5.3 应用层新增

```
application/pricing/
├── PricingPlanService.java           -- 应用服务
└── dto/
    ├── PricingPlanResponse.java      -- 响应DTO
    └── PricingPlanQueryRequest.java  -- 查询请求
```

### 5.4 适配器层新增

```
adapter/api/
└── PricingPlanController.java        -- REST API
```

### 5.5 同步机制扩展

扩展 `BuiltinMetadataSyncRunner`，支持 pricing_plans 同步：

```java
@Component
public class BuiltinMetadataSyncRunner implements ApplicationRunner {

    @Autowired
    private BuiltinMetadataLoader metadataLoader;

    @Autowired
    private BuiltinPricingLoader pricingLoader;

    @Autowired
    private ProviderMetadataGateway providerGateway;

    @Autowired
    private ModelMetadataGateway modelGateway;

    @Autowired
    private PricingPlanGateway pricingGateway;

    @Override
    public void run(ApplicationArguments args) {
        // 1. 同步供应商元数据
        syncProviders();

        // 2. 同步模型元数据
        syncModels();

        // 3. 同步定价计划（新增）
        syncPricingPlans();
    }

    private void syncPricingPlans() {
        List<PricingPlan> plans = pricingLoader.loadAll();
        for (PricingPlan plan : plans) {
            pricingGateway.saveOrUpdate(plan);
        }
    }
}
```

---

## 六、模型元数据更新清单

根据文档调研，更新以下供应商的模型信息：

| 供应商 | 当前模型（删除） | 更新为（新增） |
|--------|-----------------|---------------|
| **Anthropic** | Claude Opus 4, Sonnet 4, 3.5 Haiku, 3.5 Sonnet | Opus 4.7, Sonnet 4.6, Haiku 4.5 |
| **OpenAI** | GPT-4.1, GPT-4o, O3, O4-mini | GPT-5.5, GPT-5.4, GPT-5.4 mini, GPT-5.4 nano, O3, O3-Pro |
| **DeepSeek** | Chat, Coder, Reasoner | V4-Pro, V4-Flash, V3.2, R1 |
| **Gemini** | 2.5 Pro/Flash, 2.0 Flash, 1.5 Pro/Flash | 3.1 Pro, 3.1 Flash-Lite, 3 Flash, 2.5 Flash-Lite |
| **Qwen** | Max, Plus, Turbo, VL Max/Plus | Qwen3-Max, Qwen3.5-Plus, Qwen3-Turbo, Qwen3-Long, Qwen3-VL, Qwen3-Coder |
| **Zhipu** | GLM-4 Plus/Air/Flash/4V Plus | GLM-5.1, GLM-5, GLM-5-Turbo, GLM-4.7, GLM-4.6V |
| **Moonshot** | Kimi 8K/32K/128K | kimi-k2.6, kimi-k2.5, moonshot-v1-vision |
| **MiniMax** | (需确认现有) | M2.7, M2.5, M2.1, Hailuo-2.3, Speech-2.8 |
| **Volcengine** | (需确认现有) | Doubao-Seed-2.0 系列 |
| **Wenxin** | (需确认现有) | ERNIE-5.1, ERNIE-5.0, ERNIE-X1.1, ERNIE-4.5-Turbo |
| **Xunfei** | (需确认现有) | X2-Flash, X2, X1.5, Ultra, Max, Pro |
| **Tencent** | (需确认现有) | Hunyuan-T1, TurboS, a13b, Vision 1.5 |
| **Baichuan** | (需确认现有) | (文档未提及具体模型) |

---

## 七、定价计划数据清单

根据文档调研，新增以下定价计划：

### 7.1 国际厂商

| 供应商 | Plan Type | Plan Name | Base URL | Protocol |
|--------|-----------|-----------|----------|----------|
| **OpenAI** | STANDARD | 按量付费 | `https://api.openai.com/v1` | OPENAI |
| **OpenAI** | BATCH | 批量异步 | `https://api.openai.com/v1` | OPENAI |
| **Anthropic** | STANDARD | 按量付费 | `https://api.anthropic.com/v1` | ANTHROPIC |
| **Anthropic** | BATCH | 批量处理 | `https://api.anthropic.com/v1` | ANTHROPIC |
| **Anthropic** | CACHE | Prompt Caching | `https://api.anthropic.com/v1` | ANTHROPIC |
| **Gemini** | STANDARD | 按量付费 | `https://generativelanguage.googleapis.com/v1beta` | GEMINI |
| **Gemini** | FREE_TIER | 免费额度 | `https://generativelanguage.googleapis.com/v1beta` | GEMINI |

### 7.2 国内厂商

| 供应商 | Plan Type | Plan Name | Base URL | Protocol | Subscription Fee |
|--------|-----------|-----------|----------|----------|-----------------|
| **DeepSeek** | STANDARD | 按量付费 | `https://api.deepseek.com` | OPENAI | - |
| **DeepSeek** | STANDARD | Anthropic兼容 | `https://api.deepseek.com/anthropic` | ANTHROPIC | - |
| **DeepSeek** | CACHE | 缓存命中 | `https://api.deepseek.com` | OPENAI | - |
| **Zhipu** | STANDARD | 按量付费 | `https://open.bigmodel.cn/api/paas/v4/` | OPENAI | - |
| **Zhipu** | SUBSCRIPTION | Coding Plan Lite | `https://open.bigmodel.cn/api/coding/paas/v4` | OPENAI | ¥49/月 |
| **Zhipu** | SUBSCRIPTION | Coding Plan Pro | `https://open.bigmodel.cn/api/coding/paas/v4` | OPENAI | ¥149/月 |
| **Zhipu** | SUBSCRIPTION | Coding Plan Max | `https://open.bigmodel.cn/api/coding/paas/v4` | OPENAI | ¥469/月 |
| **Volcengine** | STANDARD | 在线推理 | `https://ark.cn-beijing.volces.com/api/v3` | OPENAI | - |
| **Volcengine** | SUBSCRIPTION | Coding Plan Lite | `https://ark.cn-beijing.volces.com/api/coding/v3` | OPENAI | ¥7.9/月 |
| **Volcengine** | SUBSCRIPTION | Coding Plan Pro | `https://ark.cn-beijing.volces.com/api/coding/v3` | OPENAI | ¥39.9/月 |
| **Volcengine** | SUBSCRIPTION | Coding Plan | `https://ark.cn-beijing.volces.com/api/coding` | ANTHROPIC | - |
| **Qwen** | STANDARD | 按量付费(北京) | `https://dashscope.aliyuncs.com/compatible-mode/v1` | OPENAI | - |
| **Qwen** | STANDARD | 按量付费(弗吉尼亚) | `https://dashscope-us.aliyuncs.com/compatible-mode/v1` | OPENAI | - |
| **Qwen** | STANDARD | 按量付费(新加坡) | `https://dashscope-intl.aliyuncs.com/compatible-mode/v1` | OPENAI | - |
| **Moonshot** | STANDARD | Tier 0 | `https://api.moonshot.cn/v1` | OPENAI | - |
| **Moonshot** | STANDARD | Tier 1 | `https://api.moonshot.cn/v1` | OPENAI | - |
| **Moonshot** | STANDARD | Tier 2 | `https://api.moonshot.cn/v1` | OPENAI | - |
| **MiniMax** | STANDARD | 国内版 | `https://api.minimaxi.com/v1` | OPENAI | - |
| **MiniMax** | STANDARD | Anthropic兼容 | `https://api.minimaxi.com/anthropic` | ANTHROPIC | - |

---

## 八、实现步骤

### Phase 1: 数据库迁移
1. 创建 V13 迁移脚本
2. 新增 `pricing_plans` 表
3. 调整 `provider_metadata` 表结构（移除 provider_config.base_url）

### Phase 2: 领域层实现
1. 新增 `PricingPlan` 实体
2. 新增 `PlanType`, `Protocol`, `Region` 枚举
3. 新增 `PricingPlanGateway` 接口
4. 新增 `PricingPlanDomainService`

### Phase 3: 基础设施层实现
1. 新增 `PricingPlanDo` 数据库对象
2. 新增 `PricingPlanRepository`
3. 新增 `PricingPlanGatewayImpl`
4. 新增 `BuiltinPricingLoader`

### Phase 4: 应用层实现
1. 新增 `PricingPlanService`
2. 新增 DTO 类

### Phase 5: 适配器层实现
1. 新增 `PricingPlanController`

### Phase 6: JSON 文件更新
1. 更新所有 `providers/*.json`（简化结构）
2. 更新所有 `models/*.json`（最新模型）
3. 新增所有 `pricing/*.json`

### Phase 7: 同步机制扩展
1. 扩展 `BuiltinMetadataSyncRunner` 支持 pricing 同步

### Phase 8: 测试
1. 单元测试
2. 集成测试
3. 启动验证同步机制

---

## 九、API 设计

### 9.1 查询供应商的定价计划

```
GET /api/v1/providers/{providerId}/pricing-plans
```

响应：
```json
{
  "provider_id": "deepseek",
  "plans": [
    {
      "plan_type": "STANDARD",
      "plan_name": "按量付费",
      "base_url": "https://api.deepseek.com",
      "protocol": "OPENAI",
      "is_default": true
    },
    {
      "plan_type": "CACHE",
      "plan_name": "缓存命中",
      "base_url": "https://api.deepseek.com",
      "protocol": "OPENAI",
      "input_price_multiplier": 0.025
    }
  ]
}
```

### 9.2 查询支持的协议类型

```
GET /api/v1/providers/{providerId}/protocols
```

响应：
```json
{
  "provider_id": "deepseek",
  "protocols": ["OPENAI", "ANTHROPIC"]
}
```

### 9.3 查询默认定价计划

```
GET /api/v1/providers/{providerId}/pricing-plans/default
```

---

## 十、风险与注意事项

1. **数据迁移风险**：现有 `provider_config.base_url` 需迁移到 `pricing_plans`
2. **向后兼容**：需确保现有 API 调用不受影响
3. **价格精度**：保持原币种，注意单位换算
4. **促销时效**：限时优惠需设置 `valid_until`，系统需自动判断有效性
5. **多端点选择**：用户调用时需明确选择套餐/端点

---

## 十一、验收标准

1. 数据库迁移成功，`pricing_plans` 表创建完成
2. 启动应用，自动同步所有 JSON 文件到数据库
3. API 可查询供应商的定价计划列表
4. API 可查询供应商支持的协议类型
5. 所有 13 家厂商的模型信息更新为最新版本
6. 所有厂商的定价计划数据完整录入

---

**设计完成，待用户审核后进入实现阶段。**