---
comet_change: supply-domain-merge
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-07-supply-domain-merge
status: final
---

# 供应域合并方案 — 技术设计文档

## Context

供应域当前存在双轨数据结构（14 个实体），Provider/Model 在 Catalog 和 Runtime 两边字段 85%~90% 重叠。物化流程同步两套数据，衍生目录实体完全冗余。ChannelModel 承载关联+定价+路由属性，职责不清。

## Goals / Non-Goals

**Goals:**
- 实体数从 14 减少到 8
- ChannelModel 语义升级为 ModelInstance，支持实例级能力覆盖和路由决策
- Channel 职责收窄为连接配置容器
- 定价数据集中在 PlanCatalog.pricing
- 物化概念替换为供给（Provision）
- 删除 source/syncedAt，简化数据追踪

**Non-Goals:**
- 枚举不合并
- providerType 不下沉到 Provider
- 不实现成本优化路由（后续优化）
- 不实现 Provider/Model 信息补全功能（后续优化）
- 前端只做 API 适配，不做 UI 重设计

## Decisions

### D1: Provider/Model 直接合并

将 ProviderCatalog 字段合并进 Provider，ModelCatalog 字段合并进 Model，删除 Catalog 实体。

**替代方案 B（引用模式）**：Provider 只保留 id+code，展示信息从 Catalog 读取 → JOIN 查询复杂、状态冲突
**替代方案 C（保留双轨）**：消除冗余字段但仍同步 → 维护负担

**选择理由**：方案 A 最简单。Provider/Model 是"身份+属性"统一概念，不需要模板-实例分离。

### D2: 定价集中在 PlanCatalog.pricing

删除 ModelInstance 上全部 7 个定价字段。定价是套餐级属性，不是实例级固有属性。运营时通过 Channel.name = planCode 反查 PlanCatalog。

**定价缓存**：当前不做缓存优化，实时解析 PlanCatalog.pricing JSON。后续如需按价格路由，在 Channel 上加 pricingSnapshot 缓存字段。

### D3: priority/weight 下沉到 ModelInstance

路由决策粒度是模型级——同一渠道不同模型可有不同路由策略。下沉后 InstanceSelector 基于单实体排序。

### D4: 物化 → 供给

ProviderCatalog/ModelCatalog 删除后，Provider/Model 不再有"从模板创建"语义。PlanCatalog → Channel 的转化是"创建"而非"物化"。

### D5: BuiltinDataLoader 直接加载运营表

内置数据直接写入 Provider/Model/PlanCatalog，upsert 逻辑简化为"按 code/modelName 检查是否存在"。

### D6: source/syncedAt 全部删除

数据来源追踪复杂度远大于当前价值。可通过 createdBy 区分系统和用户创建。代价：无法区分内置/同步/手动数据，无法自动同步更新。

## Data Model

### Provider（统一）

```
Provider
├── id: Long
├── code: String              ← 原 Catalog.providerCode
├── name: String              ← 原 Catalog.providerName
├── logoUrl: String           ← 重叠合并
├── websiteUrl: String        ← 重叠合并
├── description: String       ← 重叠合并
├── apiDocUrl: String         ← Runtime 独有
├── priority: Integer         ← Runtime 独有
├── state: ProviderState      ← 枚举不合并
└── BaseEntity fields
```

### Model（统一）

```
Model
├── id: Long
├── modelName: String         ← 两边一致
├── displayName: String
├── modelFamily: String
├── contextWindow: Integer
├── maxInputTokens: Integer
├── maxOutputTokens: Integer
├── capabilities: Map<String,Boolean>  ← 统一结构化类型
├── modalities: List<String>           ← 统一结构化类型
├── knowledgeCutoff: String            ← 从 Catalog 合并
├── state: ModelState        ← 枚举不合并
└── BaseEntity fields
```

### ModelInstance

```
ModelInstance
├── id: Long
├── channelId: Long
├── modelId: Long
├── upstreamModelName: String
├── capabilitiesOverride: Map<String,Boolean>  ← 新增
├── contextWindowOverride: Integer             ← 新增
├── priority: Integer          ← 从 Channel 下沉
├── weight: Integer            ← 从 Channel 下沉
├── quotaLimit: Long
├── state: ChannelModelState   ← 枚举不合并
└── BaseEntity fields
```

### Channel（精简）

```
Channel
├── id: Long
├── providerId: Long
├── name: String
├── billingMode: BillingMode
├── quotaLimit: Long
├── timeout: Integer
├── maxRetries: Integer
├── state: ChannelState        ← 枚举不合并
└── BaseEntity fields
```

## Service Architecture

### ChannelProvisionService

```
provisionFromPlan(planCode, request)
  ├── 查询 PlanCatalog
  ├── ensureProvider(providerCode) — 查找或创建最小信息 Provider
  ├── 检查 Channel 是否已存在
  ├── createChannel(provider, plan, request) — name=planCode
  ├── createEndpoints(channel, plan) — 解析 endpoints JSON
  ├── createModelInstances(channel, plan, providerCode)
  │     ├── 解析 pricing JSON
  │     ├── ensureModel(modelName) — 查找或创建最小信息 Model
  │     ├── resolveUpstreamModelName(providerCode, modelName)
  │     └── 创建 ModelInstance（priority=100, weight=100）
  └── createCredentials(channel, request) — 解析 request.apiKeys
```

### BuiltinDataLoader

```
CommandLineRunner (@Order 1)
  ├── loadIfNeeded()
  │     ├── providerGateway.count() == 0 → loadProviders()
  │     ├── modelGateway.count() == 0 → loadModels()
  │     ├── planCatalogGateway.count() == 0 → loadPlanCatalogs()
  │     └── planModelCatalogGateway.count() == 0 → loadPlanModelCatalogs()
  ├── loadProviders() — 从 catalog/providers.json → Provider
  └── loadModels() — 从 catalog/model-specs.json → Model
```

## API Design

| 端点 | 方法 | 说明 | 权限 |
|---|---|---|---|
| `/api/v1/provision/from-plan/{planCode}` | POST | 从套餐创建渠道 | ADMIN |
| `/api/v1/provision/batch/{providerCode}` | POST | 批量创建 | ADMIN |
| `/api/v1/plan-catalogs` | GET | 列出套餐目录 | - |
| `/api/v1/plan-catalogs/{planCode}` | GET | 套餐详情 | - |
| `/api/v1/plan-catalogs/{planCode}/pricing` | GET | 套餐定价 | - |

## Routing Impact

```
原: ModelMatcher → ChannelSelector → CredentialResolver → EndpointResolver
新: ModelMatcher → InstanceSelector → CredentialResolver → EndpointResolver

InstanceSelector:
  findActiveByModelIdOrderByPriority(modelId)
  → 按 priority 升序返回 ModelInstance 列表
  → 过滤用户团队权限（TeamChannel）
  → 过滤活跃 Channel
  → 返回优先级最高的 ModelInstance
```

## Migration Strategy

分 6 个阶段执行数据库迁移：

1. Provider 合并（迁移数据 + 删除 provider_catalogs）
2. Model 合并（新增 knowledge_cutoff + 迁移数据 + 删除 model_spec_catalogs）
3. ChannelModel → ModelInstance（重命名表 + 删除定价列 + 新增覆盖列/priority/weight + 数据迁移）
4. Channel 精简（删除 priority/weight）
5. PlanCatalog 精简（删除 source/synced_at）
6. 删除衍生目录表

每阶段独立可回滚。priority/weight 数据先从 Channel 迁移到 ModelInstance，再从 Channel 删除。

## Test Strategy

| 层级 | 测试内容 |
|---|---|
| 单元 | ModelInstance 创建、capabilitiesOverride 覆盖逻辑、contextWindowOverride 覆盖逻辑 |
| 集成 | ChannelProvisionService 从 PlanCatalog 创建完整渠道链 |
| 路由 | InstanceSelector 按 priority 排序、权限过滤、upstreamModelName 替换 |
| API | 新端点 CRUD、ADMIN 权限控制 |
| 迁移 | 数据从旧表正确迁移到新表 |

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 定价查询性能 | 后续优化：Channel.pricingSnapshot 缓存字段 |
| Provider/Model 信息不完整 | 后续优化：提供"补全"功能 |
| DB 迁移停机 | 分阶段执行，每阶段可回滚 |
| 前端改动面大 | 先 API 层，再逐步前端 |
| 放弃 models.dev 同步 | 后续可重新引入独立同步服务 |
