# Channel Provision
## Summary

## Purpose

Define the channel provisioning mechanism that supports creating channels from plan catalogs, with automatic cascade creation of missing providers and models, and inline provider creation support.
## Requirements
### Requirement: 从套餐创建渠道
系统 SHALL 提供 ChannelProvisionService，支持从 PlanCatalog 创建 Channel + ChannelEndpoint + ModelInstance。创建过程中自动级联创建缺失的 Provider 和 Model（使用最小信息）。整个创建过程 SHALL 在单一数据库事务内完成，任何步骤失败均整体回滚。

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

#### Scenario: 创建过程任意步骤失败整体回滚
- **WHEN** 创建 Channel / ChannelEndpoint / ModelInstance / ChannelCredential / 级联 Provider / 级联 Model 任意步骤抛出异常
- **THEN** 数据库事务回滚，不会出现部分创建的实体或孤儿 Provider

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

### Requirement: 内联创建供应商的事务性渠道供给

系统 SHALL 在 `provisionFromPlan` 流程中支持"内联创建供应商 + 渠道"的事务原子性，避免控制台向导中途取消时残留孤儿 Provider。

#### Scenario: 控制台向导内联创建路径
- **WHEN** 客户端调用 provision API 时携带 inlineProvider 参数（包含 code/name/description 等字段），且 providerCode 在数据库不存在
- **THEN** 系统在单个事务内先创建 Provider，再创建 Channel + ChannelEndpoint + ModelInstance + ChannelCredential

#### Scenario: 内联创建过程中失败回滚
- **WHEN** 内联创建供应商或后续渠道创建过程中任意步骤抛出异常
- **THEN** 整个事务回滚，数据库不会出现孤儿 Provider 或部分创建的渠道

#### Scenario: providerCode 已存在时忽略 inlineProvider
- **WHEN** 客户端传入 inlineProvider 但 providerCode 对应的 Provider 已存在
- **THEN** 系统忽略 inlineProvider 字段，按现有"级联创建 Provider"逻辑使用已有 Provider

