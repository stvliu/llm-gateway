# Model Instance
## Purpose

模型实例能力——ModelInstance 为 Channel×Model 的运行实例，承载路由优先级与候选列表产出，供 L1 转移逐个尝试。
## Requirements
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

### Requirement: ModelInstanceRepository 接口
系统 SHALL 提供 ModelInstanceRepository 接口，包含以下方法：save、findById、findByChannelId、findActiveByChannelId、findActiveByModelId、findActiveByModelIdAndChannelIds、existsByChannelIdAndModelId、saveAll、deleteById。

#### Scenario: 按模型与渠道集合查询活跃实例
- **WHEN** 调用 findActiveByModelIdAndChannelIds(modelId, channelIds)
- **THEN** 返回该模型在指定渠道集合内的活跃 ModelInstance，按 priority 升序排序

### Requirement: InstanceSelector.select 返回排序候选列表

`InstanceSelector.select` SHALL 返回按 `ApplicationChannel.priority` 升序排序的候选列表，供 L1 故障转移逐个尝试。

**要点**:
- `select` 返回 `List<ModelInstance>`，按应用级 `priority` 升序排序，不收敛到单实例
- 数据面认证即授权：授权蕴含在认证（`Identity.applicationId` 即 Key 绑定的应用），`select` 读取应用渠道配置并带渠道条件查询活跃实例
- 候选列表经 `RouterChain` 纯路由链产出：`HealthRouter(@200) → PriorityRouter(@300)`（已删除 `PermissionRouter`/`LoadBalanceRouter`/`ClusterAffinity`/`PinnedModel` 路由器）

**方法签名**:
```
List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                           RoutingStrategy strategy, Protocol protocol)
```

**规则**:
- 候选按应用级 `ApplicationChannel.priority` 升序排序（由 `PriorityRouter` 排序器保证，null 回退默认值 100）
- 无可用实例时抛 `ResourceNotFoundException`
- `RoutingRequest` 携带 `applicationId` + `protocol` + `channelPriorityMap`（无容灾画像字段，ResilienceProfile 退场；应用级 `failureStrategy`/`timeout` 经 `RoutingContext` 透传）

#### Scenario: 返回排序候选列表供 L1 逐个尝试

- **WHEN** `InstanceSelector.select(modelId, applicationId, userId, role, strategy, protocol)` 被调用
- **THEN** 系统 SHALL 返回按 `priority` 升序的候选 `ModelInstance` 列表
- **THEN** 列表 SHALL 供 `ChannelFailoverInvoker` 逐个尝试
- **THEN** 系统 SHALL NOT 收敛到单实例

#### Scenario: 无可用实例抛 ResourceNotFoundException

- **WHEN** 路由链过滤后候选列表为空
- **THEN** 系统 SHALL 抛出 `ResourceNotFoundException`

