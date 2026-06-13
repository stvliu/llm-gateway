# Channel Provision
## Summary

## Requirements

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
