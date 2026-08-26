# Comet Design Handoff

- Change: supply-domain-merge
- Phase: design
- Mode: compact
- Context hash: f014eb80cbad99210e81d3fdde53b8819dddc53333ca5323015e21b1c0d02cf9

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/supply-domain-merge/proposal.md

- Source: openspec/changes/supply-domain-merge/proposal.md
- Lines: 1-39
- SHA256: 16a77b0a4b22047e6c36f20a65f6c20c36b3d7fd25555486cb6fad4898f06b6d

```md
## Why

供应域存在 14 个实体、双轨数据结构（Catalog + Runtime），导致 Provider/Model 在两边字段 85%~90% 重叠，物化流程需同步两套数据，衍生目录实体（ChannelCatalog/ChannelModelCatalog/ChannelEndpointCatalog）完全冗余。合并后消除数据冗余、简化维护，并将 ChannelModel 语义升级为 ModelInstance 以支持实例级能力覆盖和路由决策。

## What Changes

- **BREAKING** 合并 ProviderCatalog → Provider，删除 ProviderCatalog 实体
- **BREAKING** 合并 ModelCatalog → Model，删除 ModelCatalog 实体
- **BREAKING** 重命名 ChannelModel → ModelInstance，删除全部 7 个定价字段（inputPrice/outputPrice/reasoningPrice/cacheReadPrice/cacheWritePrice/inputAudioPrice/outputAudioPrice）
- ModelInstance 新增 capabilitiesOverride（Map<String,Boolean>）和 contextWindowOverride（Integer）字段，支持实例级能力/规格覆盖
- Channel.priority/weight 下沉到 ModelInstance，Channel 职责收窄为连接配置容器
- Model 新增 knowledgeCutoff 字段（从 ModelCatalog 合并）
- PlanCatalog/PlanModelCatalog 删除 source/syncedAt 字段，放弃数据来源追踪
- 删除 CatalogSource、ProviderType 枚举
- 删除衍生目录实体：ChannelCatalog、ChannelModelCatalog、ChannelEndpointCatalog
- 删除物化概念，替换为 ChannelProvisionService（从 PlanCatalog 创建 Channel）
- 删除 CatalogMaterializeService、CatalogSyncService、ModelsDevSyncClient、CatalogDomainService
- 重构 BuiltinCatalogLoader → BuiltinDataLoader（直接加载 Provider/Model/PlanCatalog）
- 删除 CatalogController，新建 PlanCatalogController、ChannelProvisionController
- 定价数据唯一来源变更为 PlanCatalog.pricing (JSON)
- 前端目录页面重构，物化按钮改为"创建渠道"

## Capabilities

### New Capabilities
- `model-instance`: 模型实例概念——模型在某渠道上的具体化身，支持实例级能力覆盖、上下文窗口覆盖、路由优先级/权重
- `channel-provision`: 从套餐目录（PlanCatalog）创建渠道（Channel）的供给流程，替代原物化概念

### Modified Capabilities

## Impact

- **领域层**：Provider/Model 实体字段扩展，ChannelModel 重命名为 ModelInstance 并大幅精简，Channel 删除 priority/weight
- **应用层**：删除 CatalogMaterializeService/CatalogSyncService/CatalogService，新建 ChannelProvisionService/PlanCatalogService
- **基础设施层**：删除 5 个 Catalog Gateway 实现、5 个 JPA Repository、5 个 DO 类，新增 ModelInstance 相关基础设施
- **适配器层**：删除 CatalogController，新建 PlanCatalogController/ChannelProvisionController
- **数据库**：6 张表 DDL 变更 + 5 张表删除 + 数据迁移
- **前端**：目录页面重构，API 端点变更
- **路由**：ChannelSelector 重构为 InstanceSelector，基于 ModelInstance 单实体做路由决策
```

## openspec/changes/supply-domain-merge/design.md

- Source: openspec/changes/supply-domain-merge/design.md
- Lines: 1-74
- SHA256: 9fc8a31334101f07e6484387566a638def48a988e2cd1054f4e1efbf636998ef

```md
## Context

供应域（Supply Domain）当前存在双轨数据结构：Catalog 层（7 个实体）和 Runtime 层（7 个实体），其中 Provider/Model 的 Catalog 和 Runtime 字段 85%~90% 重叠。物化流程（CatalogMaterializeService）负责从 Catalog 复制数据到 Runtime，增加了系统复杂度。ChannelModel 承载关联+定价+路由属性，职责不清。衍生目录实体（ChannelCatalog/ChannelModelCatalog/ChannelEndpointCatalog）仅为 Runtime 的只读投影，无独立价值。

## Goals / Non-Goals

**Goals:**
- 实体数从 14 减少到 8，消除双轨数据冗余
- ChannelModel 语义升级为 ModelInstance，支持实例级能力覆盖和路由决策
- Channel 职责收窄为连接配置容器，priority/weight 下沉到 ModelInstance
- 定价数据集中在 PlanCatalog.pricing，消除 ModelInstance 上的定价字段
- 物化概念替换为"从套餐创建渠道"（ChannelProvisionService）
- 删除 source/syncedAt，简化数据追踪

**Non-Goals:**
- 枚举不合并（ProviderState/ModelState/ChannelState 等各自保留）
- providerType 不下沉到 Provider（随 ProviderCatalog 一起删除）
- 不实现成本优化路由（按价格排序 ModelInstance），后续优化
- 不实现 Provider/Model 信息补全功能（物化后只创建最小信息），后续优化
- 前端只做 API 适配，不做 UI 重设计

## Decisions

### D1: Provider/Model 合并策略 — 直接合并（非引用模式）

**选择**：将 ProviderCatalog 字段合并进 Provider，ModelCatalog 字段合并进 Model，然后删除 Catalog 实体。

**替代方案**：
- B: Provider/Model 退化为引用（只保留 id + code，展示信息从 Catalog 读取）— 查询需要 JOIN，运营状态和目录状态冲突
- C: 保留双轨但消除冗余字段 — 仍有同步问题

**理由**：方案 A 最简单，单表查询，无同步问题。Provider/Model 本身就是"身份+属性"的统一概念，不需要模板-实例分离。

### D2: ModelInstance 定价字段删除 — 定价集中在 PlanCatalog.pricing

**选择**：删除 ModelInstance 上全部 7 个定价字段，定价数据唯一来源为 PlanCatalog.pricing (JSON)。

**理由**：定价是套餐级别的属性，不是模型实例的固有属性。同一模型在不同套餐中定价不同，定价应随套餐走。运营时通过 Channel.name = planCode 反查 PlanCatalog 获取定价。

**代价**：路由时如需按价格排序，需额外查询 PlanCatalog。可后续通过 Channel 上的 pricingSnapshot 缓存字段优化。

### D3: Channel.priority/weight 下沉到 ModelInstance

**选择**：priority/weight 从 Channel 移到 ModelInstance。

**理由**：路由决策的粒度是模型级别的——同一渠道的不同模型可能有不同的路由策略。下沉后 InstanceSelector 基于单实体排序，无需跨 Channel + ChannelModel 拼凑。

### D4: 物化概念替换为供给（Provision）

**选择**：删除 CatalogMaterializeService，新建 ChannelProvisionService。

**理由**：ProviderCatalog/ModelCatalog 删除后，Provider/Model 不再有"从模板创建"的语义，物化概念自然消失。PlanCatalog 仍然保留，其转化为 Channel 的过程是"从套餐创建渠道"，语义更直观。

### D5: BuiltinDataLoader 直接加载运营表

**选择**：BuiltinCatalogLoader 重构为 BuiltinDataLoader，直接加载 Provider/Model/PlanCatalog。

**理由**：没有 Catalog 中间层，内置数据直接写入运营表。upsert 逻辑简化为"按 code/modelName 检查是否存在，不存在则创建"。

### D6: source/syncedAt 全部删除

**选择**：所有实体的 source/syncedAt 字段删除，CatalogSource 枚举删除。

**理由**：数据来源追踪的复杂度（优先级覆盖、标记废弃、同步流程）远大于其当前价值。可通过 createdBy 区分系统创建和用户创建。

**代价**：无法区分内置数据、models.dev 同步数据、手动创建数据；无法从 models.dev 自动同步更新。

## Risks / Trade-offs

- [定价查询性能] 反查 PlanCatalog.pricing JSON 比直接查 ModelInstance 定价字段慢 → 可在 Channel 上加 pricingSnapshot 缓存字段（后续优化）
- [Provider/Model 信息不完整] 从 PlanCatalog 级联创建 Provider/Model 时只有最小信息（code/name）→ 用户需手动补充，或后续提供"补全"功能
- [数据库迁移停机] DDL 变更涉及 6 张表修改 + 5 张表删除 → 分阶段执行，每阶段独立可回滚
- [前端改动面大] 目录页面多个组件需重构 → 先完成 API 层，再逐步迁移前端
- [放弃 models.dev 同步] 删除 source 追踪后无法自动同步上游数据 → 如需恢复，后续可重新引入独立同步服务
```

## openspec/changes/supply-domain-merge/tasks.md

- Source: openspec/changes/supply-domain-merge/tasks.md
- Lines: 1-127
- SHA256: 546afe7cb0dc068b0b65d7ce9a2286be80bdb827343a160286cf134f4eea2e3e

[TRUNCATED]

```md
## 1. 枚举与基础设施清理

- [ ] 1.1 删除 CatalogSource 枚举类及所有引用
- [ ] 1.2 删除 ProviderType 枚举类及所有引用

## 2. Provider 合并

- [ ] 2.1 Provider 实体新增字段对齐（确认 code/name/logoUrl/websiteUrl/description/apiDocUrl/priority/state 完整）
- [ ] 2.2 删除 ProviderCatalog 实体
- [ ] 2.3 删除 ProviderCatalogGateway 接口
- [ ] 2.4 删除 ProviderCatalogGatewayImpl 基础设施实现
- [ ] 2.5 删除 ProviderCatalogRepository JPA 接口
- [ ] 2.6 删除 ProviderCatalogDo JPA 实体
- [ ] 2.7 ProviderGateway 新增 existsByCode、findByKeyword 方法
- [ ] 2.8 ProviderGatewayImpl 实现新增方法

## 3. Model 合并

- [ ] 3.1 Model 实体新增 knowledgeCutoff 字段
- [ ] 3.2 Model.capabilities/modalities 统一为结构化类型（Map/List），基础设施层处理 JSON 转换
- [ ] 3.3 删除 ModelCatalog 实体
- [ ] 3.4 删除 ModelCatalogGateway 接口
- [ ] 3.5 删除 ModelCatalogGatewayImpl 基础设施实现
- [ ] 3.6 删除 ModelCatalogRepository JPA 接口
- [ ] 3.7 删除 ModelCatalogDo JPA 实体
- [ ] 3.8 ModelGateway 新增 existsByModelName、findByKeyword、findByCapability 方法
- [ ] 3.9 ModelGatewayImpl 实现新增方法

## 4. ChannelModel → ModelInstance

- [ ] 4.1 创建 ModelInstance 实体（channelId, modelId, upstreamModelName, capabilitiesOverride, contextWindowOverride, priority, weight, quotaLimit, state）
- [ ] 4.2 创建 ModelInstanceGateway 接口（含 findActiveByModelIdOrderByPriority）
- [ ] 4.3 创建 ModelInstanceDo JPA 实体（表名 model_instances）
- [ ] 4.4 创建 ModelInstanceRepository JPA 接口
- [ ] 4.5 创建 ModelInstanceGatewayImpl 基础设施实现
- [ ] 4.6 删除 ChannelModel 实体
- [ ] 4.7 删除 ChannelModelGateway 接口
- [ ] 4.8 删除 ChannelModelDo JPA 实体
- [ ] 4.9 删除 ChannelModelRepository JPA 接口
- [ ] 4.10 删除 ChannelModelGatewayImpl 基础设施实现
- [ ] 4.11 所有引用 ChannelModel 的代码改为 ModelInstance

## 5. Channel 精简

- [ ] 5.1 Channel 实体删除 priority、weight 字段
- [ ] 5.2 ChannelDo JPA 实体删除 priority、weight 列映射
- [ ] 5.3 ChannelGatewayImpl 删除 priority/weight 相关转换
- [ ] 5.4 ChannelCreateRequest/ChannelUpdateRequest DTO 删除 priority/weight
- [ ] 5.5 ChannelResponse DTO 删除 priority/weight

## 6. PlanCatalog 精简

- [ ] 6.1 PlanCatalog 实体删除 source、syncedAt 字段
- [ ] 6.2 PlanModelCatalog 实体删除 source、syncedAt 字段
- [ ] 6.3 PlanCatalogDo/PlanModelCatalogDo JPA 实体删除对应列映射
- [ ] 6.4 PlanCatalogGateway 删除 source 相关方法（findBySource、findBySourceExcludingKeys）
- [ ] 6.5 PlanModelCatalogGateway 删除 source 相关方法
- [ ] 6.6 PlanCatalogGatewayImpl/PlanModelCatalogGatewayImpl 删除对应实现

## 7. 衍生目录删除

- [ ] 7.1 删除 ChannelCatalog 实体及 Gateway/Repository/DO
- [ ] 7.2 删除 ChannelModelCatalog 实体及 Gateway/Repository/DO
- [ ] 7.3 删除 ChannelEndpointCatalog 实体及 Gateway/Repository/DO

## 8. 服务层重构

- [ ] 8.1 删除 CatalogMaterializeService 及相关 DTO（MaterializeResult/MaterializeBatchResult/PlanResult/MaterializeBatchRequest/MaterializePlanRequest）
- [ ] 8.2 删除 CatalogSyncService
- [ ] 8.3 删除 CatalogService/CatalogServiceImpl 及相关 DTO（ProviderCatalogResponse/PlanCatalogResponse/ModelCatalogResponse/PlanDetailResponse）
- [ ] 8.4 删除 ModelsDevSyncClient
- [ ] 8.5 删除 CatalogDomainService
- [ ] 8.6 创建 ChannelProvisionService（provisionFromPlan、provisionBatch、ensureProvider、ensureModel）
- [ ] 8.7 创建 ProvisionResult/BatchProvisionResult/ProvisionRequest/BatchProvisionRequest DTO
- [ ] 8.8 创建 PlanCatalogService（listPlanCatalogs、getPlanDetail、getPricing）

## 9. BuiltinDataLoader 重构

- [ ] 9.1 重构 BuiltinCatalogLoader → BuiltinDataLoader
- [ ] 9.2 loadProviders() 改为直接加载 Provider（从 providers.json）
```

Full source: openspec/changes/supply-domain-merge/tasks.md

## openspec/changes/supply-domain-merge/specs/channel-provision/spec.md

- Source: openspec/changes/supply-domain-merge/specs/channel-provision/spec.md
- Lines: 1-72
- SHA256: 785b03dec05dac33fa227e9f33d9cb74f11d4e3b88b4270e7d3dd76062b0ae72

```md
## ADDED Requirements

### Requirement: 从套餐创建渠道
系统 SHALL 提供 ChannelProvisionService，支持从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance。创建过程中自动级联创建缺失的 Provider 和 Model（使用最小信息）。

#### Scenario: 正常创建渠道
- **WHEN** 管理员调用 provisionFromPlan(planCode, request)
- **THEN** 系统创建 Channel（name=planCode，关联 Provider）、ChannelEndpoint（从 endpoints JSON 解析）、ModelInstance（从 pricing JSON 解析，级联创建缺失的 Model）

#### Scenario: 级联创建 Provider
- **WHEN** PlanCatalog.providerCode 对应的 Provider 不存在
- **THEN** 系统自动创建 Provider（code=providerCode，name=providerCode，priority=100，state=ACTIVE）

#### Scenario: 级联创建 Model
- **WHEN** pricing JSON 中某个 modelName 对应的 Model 不存在
- **THEN** 系统自动创建 Model（modelName=modelName，displayName=modelName，state=ACTIVE）

#### Scenario: 渠道已存在
- **WHEN** Channel 已存在（providerId + name 匹配）
- **THEN** 系统返回 skipped 状态，不重复创建

#### Scenario: 创建渠道时传入 API Key
- **WHEN** request 包含 apiKeys 列表
- **THEN** 系统为创建的 Channel 批量创建 ChannelCredential

### Requirement: 批量创建渠道
系统 SHALL 支持按供应商批量创建渠道，从该供应商下所有 PlanCatalog 逐一创建 Channel。

#### Scenario: 批量创建
- **WHEN** 管理员调用 provisionBatch(providerCode, request)
- **THEN** 系统查询该供应商下所有 PlanCatalog，逐一调用 provisionFromPlan，返回成功/跳过/失败统计

#### Scenario: 指定 planCodes 过滤
- **WHEN** request 包含 planCodes 列表
- **THEN** 系统只创建指定 planCode 对应的渠道

### Requirement: 套餐目录查询
系统 SHALL 提供 PlanCatalogService，支持列出套餐目录、获取套餐详情、获取套餐定价。

#### Scenario: 列出套餐目录
- **WHEN** 调用 listPlanCatalogs(providerCode)
- **THEN** 返回指定供应商（或全部）的 PlanCatalog 列表，包含 provisioned 标志（通过 Channel 是否存在判断）

#### Scenario: 获取套餐详情
- **WHEN** 调用 getPlanDetail(planCode)
- **THEN** 返回 PlanCatalog 详情，包含解析后的 endpoints 列表和 pricing 列表

#### Scenario: 获取套餐定价
- **WHEN** 调用 getPricing(planCode)
- **THEN** 返回解析后的定价列表（从 pricing JSON 解析）

### Requirement: 渠道供给 API
系统 SHALL 提供 /api/v1/provision 端点，包含 POST /from-plan/{planCode}（单个创建）和 POST /batch/{providerCode}（批量创建），仅 ADMIN 角色可访问。

#### Scenario: 单个创建渠道
- **WHEN** ADMIN 用户 POST /api/v1/provision/from-plan/{planCode}
- **THEN** 系统调用 ChannelProvisionService.provisionFromPlan，返回 ProvisionResult

#### Scenario: 非 ADMIN 用户调用
- **WHEN** 非 ADMIN 用户调用供给 API
- **THEN** 系统返回 403 Forbidden

### Requirement: 套餐目录 API
系统 SHALL 提供 /api/v1/plan-catalogs 端点，支持列出套餐（GET /）、获取详情（GET /{planCode}）、获取定价（GET /{planCode}/pricing）。

#### Scenario: 列出套餐
- **WHEN** GET /api/v1/plan-catalogs?providerCode=openai
- **THEN** 返回 openai 供应商下的套餐列表

#### Scenario: 获取定价
- **WHEN** GET /api/v1/plan-catalogs/openai_gpt4o_payg/pricing
- **THEN** 返回该套餐的定价列表
```

## openspec/changes/supply-domain-merge/specs/model-instance/spec.md

- Source: openspec/changes/supply-domain-merge/specs/model-instance/spec.md
- Lines: 1-66
- SHA256: aad3bb04a3f26890cd501b261d7bc2180fdc321e2cfeb230a2cbb32373505263

```md
## ADDED Requirements

### Requirement: ModelInstance 实体定义
系统 SHALL 提供 ModelInstance 实体作为"模型在某渠道上的具体化身"，包含以下字段：channelId（关联 Channel）、modelId（关联 Model）、upstreamModelName（上游模型名映射）、capabilitiesOverride（实例级能力覆盖，Map<String,Boolean>）、contextWindowOverride（实例级上下文窗口覆盖）、priority（路由优先级）、weight（负载均衡权重）、quotaLimit（实例级配额）、state（ChannelModelState）。

#### Scenario: 创建 ModelInstance
- **WHEN** 管理员为渠道添加模型实例
- **THEN** 系统创建 ModelInstance 记录，关联指定 Channel 和 Model，设置 upstreamModelName 和优先级/权重

#### Scenario: upstreamModelName 为 null
- **WHEN** 创建 ModelInstance 时 upstreamModelName 为 null 或空
- **THEN** 出站调谐阶段使用 Model.modelName 作为上游模型名

### Requirement: 实例级能力覆盖
ModelInstance 的 capabilitiesOverride 字段 SHALL 支持覆盖 Model.capabilities 的默认值。当 capabilitiesOverride 中包含某个能力键时，使用覆盖值；当不包含时，使用 Model 的默认值。

#### Scenario: 覆盖模型默认能力
- **WHEN** Model.capabilities = {vision: true, tool_use: true}，ModelInstance.capabilitiesOverride = {vision: false}
- **THEN** 该实例的有效能力为 {vision: false, tool_use: true}

#### Scenario: 无覆盖时使用模型默认值
- **WHEN** ModelInstance.capabilitiesOverride 为 null 或空
- **THEN** 该实例的有效能力与 Model.capabilities 完全一致

### Requirement: 实例级上下文窗口覆盖
ModelInstance 的 contextWindowOverride 字段 SHALL 支持覆盖 Model.contextWindow 的默认值。当 contextWindowOverride 不为 null 时，使用覆盖值；为 null 时，使用 Model.contextWindow。

#### Scenario: 覆盖模型默认上下文窗口
- **WHEN** Model.contextWindow = 128000，ModelInstance.contextWindowOverride = 64000
- **THEN** 该实例的有效上下文窗口为 64000

#### Scenario: 无覆盖时使用模型默认值
- **WHEN** ModelInstance.contextWindowOverride 为 null
- **THEN** 该实例的有效上下文窗口与 Model.contextWindow 一致

### Requirement: 路由优先级在 ModelInstance 级别
路由决策 SHALL 基于 ModelInstance.priority 和 ModelInstance.weight，而非 Channel 级别。系统 SHALL 支持"同一渠道的不同模型实例有不同的优先级/权重"。

#### Scenario: 同渠道不同模型实例不同优先级
- **WHEN** 渠道 A 的 ModelInstance(gpt-4o).priority = 10，渠道 A 的 ModelInstance(claude-opus).priority = 50
- **THEN** 路由选择 gpt-4o 实例时优先级更高

#### Scenario: 按 priority 排序选择实例
- **WHEN** 用户请求某模型，存在多个 ModelInstance
- **THEN** 系统按 priority 升序排序，优先选择 priority 最小的实例

### Requirement: ModelInstance 不承载定价字段
ModelInstance 实体 SHALL NOT 包含任何定价字段（inputPrice、outputPrice、reasoningPrice、cacheReadPrice、cacheWritePrice、inputAudioPrice、outputAudioPrice）。定价数据唯一来源为 PlanCatalog.pricing (JSON)。

#### Scenario: 查询模型实例定价
- **WHEN** 需要查询某渠道中某模型的定价
- **THEN** 系统通过 Channel.name 匹配 PlanCatalog.planCode，解析 PlanCatalog.pricing JSON 获取定价

### Requirement: Channel 不再承载 priority/weight
Channel 实体 SHALL NOT 包含 priority 和 weight 字段。路由优先级和负载均衡权重完全由 ModelInstance 承载。

#### Scenario: 创建渠道时无需设置优先级
- **WHEN** 管理员创建渠道
- **THEN** 渠道配置只包含连接参数（timeout、maxRetries）、计费模式（billingMode）和总配额（quotaLimit）

### Requirement: ModelInstanceGateway 接口
系统 SHALL 提供 ModelInstanceGateway 接口，包含以下方法：save、findById、findByChannelId、findActiveByChannelId、findActiveByModelId、findActiveByModelIdOrderByPriority、existsByChannelIdAndModelId、saveAll、deleteById。

#### Scenario: 按优先级排序查询活跃实例
- **WHEN** 调用 findActiveByModelIdOrderByPriority(modelId)
- **THEN** 返回该模型所有活跃的 ModelInstance，按 priority 升序排序
```

