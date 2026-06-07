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
