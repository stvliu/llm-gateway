## ADDED Requirements

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

## MODIFIED Requirements

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
