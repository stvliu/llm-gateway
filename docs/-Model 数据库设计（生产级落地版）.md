# 大模型计费系统 Provider\-PricingPlan\-Model 数据库设计（生产级落地版）

## 一、设计核心说明

遵循行业标准三层建模：**Provider（厂商）→ PricingPlan（计费方案）→ Model（模型实例）**

完全适配：OpenAI/Anthropic 双协议、按量/订阅/缓存/批量/Coding/Agent 多套餐、多 BaseUrl、限流、折扣、有效期、场景特权

核心关系：**一个 Provider 包含多个 PricingPlan，一个 PricingPlan 绑定多个 Model**

## 二、数据库表结构（MySQL 8\.0 规范）

### 1\. llm\_provider 大模型厂商表

存储全局厂商基础信息、协议类型、全局配置

```sql
CREATE TABLE `llm_provider` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_code` varchar(64) NOT NULL COMMENT '厂商唯一编码：openai/anthropic/deepseek/qwen/glm',
  `provider_name` varchar(64) NOT NULL COMMENT '厂商名称',
  `region` varchar(32) NOT NULL COMMENT '区域：国内/国外',
  `support_protocol` varchar(128) NOT NULL COMMENT '兼容协议：openai/anthropic/both',
  `official_base_url` varchar(255) DEFAULT '' COMMENT '官方默认BaseUrl',
  `official_doc_url` varchar(255) DEFAULT '' COMMENT '官方文档地址',
  `sort` int DEFAULT 0 COMMENT '排序权重',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_code` (`provider_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型厂商表';
```

### 2\. llm\_pricing\_plan 计费方案表（核心）

对应行业 **PricingPlan**，存储同一厂商下多套差异化计费方案，包含按量、订阅、Coding、Agent、缓存、批量等所有场景

```sql
CREATE TABLE `llm_pricing_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `provider_id` bigint NOT NULL COMMENT '关联厂商ID',
  `plan_code` varchar(64) NOT NULL COMMENT '方案唯一编码',
  `plan_name` varchar(128) NOT NULL COMMENT '方案名称：标准按量/Coding Lite/Coding Pro/Agent订阅/缓存特价/批量Batch',
  `plan_type` varchar(32) NOT NULL COMMENT '方案类型：token_usage/subscription/cache/batch/coding/agent/free',
  `base_url` varchar(255) NOT NULL COMMENT '该方案专属BaseUrl（关键：不同套餐不同域名）',
  `monthly_fee` decimal(10,2) DEFAULT 0.00 COMMENT '月订阅费，0=无订阅',
  `discount_rate` decimal(5,2) DEFAULT 1.00 COMMENT '折扣比例：1.0=原价 0.5=五折',
  `quota_limit` bigint DEFAULT 0 COMMENT '月度配额，0不限',
  `qps_limit` int DEFAULT 0 COMMENT 'QPS限流，0不限',
  `valid_start_time` datetime DEFAULT NULL COMMENT '生效时间',
  `valid_end_time` datetime DEFAULT NULL COMMENT '失效时间',
  `is_free_plan` tinyint NOT NULL DEFAULT 0 COMMENT '是否免费套餐：0否 1是',
  `description` varchar(512) DEFAULT '' COMMENT '方案描述、特权说明',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plan_code` (`plan_code`),
  KEY `idx_provider_id` (`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型计费方案表';
```

### 3\. llm\_model 模型实例表

绑定具体模型名称、价格、上下文、能力标签，一个 PricingPlan 可关联多个模型

```sql
CREATE TABLE `llm_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plan_id` bigint NOT NULL COMMENT '关联计费方案ID',
  `model_name` varchar(128) NOT NULL COMMENT '官方模型名：gpt-5.5、qwen3-max、glm-5',
  `display_name` varchar(128) DEFAULT '' COMMENT '前端展示名称',
  `input_price` decimal(12,6) NOT NULL DEFAULT 0.000000 COMMENT '输入单价/百万Token',
  `output_price` decimal(12,6) NOT NULL DEFAULT 0.000000 COMMENT '输出单价/百万Token',
  `cache_input_price` decimal(12,6) DEFAULT 0.000000 COMMENT '缓存输入单价',
  `context_window` int DEFAULT 0 COMMENT '最大上下文长度',
  `support_stream` tinyint NOT NULL DEFAULT 1 COMMENT '是否支持流式',
  `support_function_call` tinyint NOT NULL DEFAULT 0 COMMENT '是否支持工具调用',
  `support_multimodal` tinyint NOT NULL DEFAULT 0 COMMENT '是否多模态',
  `price_currency` varchar(8) NOT NULL DEFAULT 'CNY' COMMENT '货币：CNY/USD',
  `is_free` tinyint NOT NULL DEFAULT 0 COMMENT '是否免费模型',
  `sort` int DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型明细表';
```

## 三、核心字段业务释义（解决建模疑惑）

### 1\. plan\_type 枚举全覆盖（行业最全）

- `token\_usage`：标准按量计费计划（默认）

- `cache`：缓存特价计划（DeepSeek/OpenAI 专属）

- `batch`：Batch批量异步5折计划

- `subscription`：通用月度订阅计划

- `coding`：Coding专属计划（阿里、智谱独有）

- `agent`：Agent智能体专项计划

- `free`：永久免费额度计划

### 2\. 为什么 BaseUrl 放在 PricingPlan 层级？

关键业务特征：**同一厂商、同一模型，不同套餐域名不同**

- 阿里云通义千问：标准按量域名 / Coding 专属域名不同

- 智谱 GLM：普通调用 Endpoint / Coding 专属 Endpoint 隔离

- 海外厂商：批量 Batch 接口域名与普通接口隔离

✅ 符合真实业务：**套餐维度隔离接入地址、权限、调度优先级**

## 四、层级关系业务示例（真实落地案例）

### 示例：阿里云通义千问

Provider：阿里云通义千问

├─ PricingPlan1：标准按量计划（通用BaseUrl、按量计费）

├─ PricingPlan2：Coding\-Lite 包月计划（专属Url、¥7\.9/月）

├─ PricingPlan3：Coding\-Pro 包月计划（专属Url、¥39\.9/月）

└─ PricingPlan4：Agent 智能体专项计划

每个 Plan 下挂载：Qwen3\-Max、Qwen3\.5\-Plus、Qwen3\-Turbo 等模型

## 五、架构优势（适配你当前调研数据）

1. **完美兼容多协议**：支持 OpenAI / Anthropic 双协议标记

2. **解决多套餐混乱问题**：同一模型多价格、多域名、多权限完全隔离

3. **覆盖所有厂商定价规则**：缓存价、批量价、订阅费、免费额度、QPS限流、时间有效期

4. **标准化可对接前端/计费/网关**：字段语义统一，无歧义

> （注：文档部分内容可能由 AI 生成）
