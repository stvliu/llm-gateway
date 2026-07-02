# Channel Health Tracking Delta Spec

## MODIFIED Requirements

### Requirement: 熔断 key 统一为 endpointId

熔断 key SHALL 统一为 `endpointId`，路由侧（`HealthRouter`）与调用侧（`KeyFailoverInvoker`）共享同一熔断器实例（`ChannelEndpointCircuitBreakerManager`）。

**变更要点**:
- 原 `HealthRouter` 用 `channelId` 查熔断，`KeyFailoverInvoker` 用 `endpointId`，路由侧与调用侧熔断互不可见
- 现 `HealthRouter`（`@Order(200)`）用 `channelId + protocol` 经 `EndpointResolver` 派生 `endpointId` 后查 `ChannelEndpointCircuitBreakerManager`
- 与 `KeyFailoverInvoker`（用 `RoutingContext.channelEndpointId()`）共享同一 manager bean
- 不向 `ModelInstance`/`model_instances` 表加 `endpointId` 字段（channel 粒度与 channel×protocol 端点粒度 1:1 不自洽，采用运行时派生方案）

**RouterChain 顺序修正**（删除 ClusterAffinity 与 PinnedModel）:
- 原 `Permission(@100) → Health(@200) → ClusterAffinity(@250) → Priority(@300) → PinnedModel(@350) → LoadBalance(@9999)`
- 现 `Permission(@100) → EndpointHealth(@200) → Priority(@300) → LoadBalance(@9999)`
- Health 先于 Priority（次优先级健康渠道能成为转移候选）
- Priority 为排序器（输出完整候选列表，不收敛），非选择器

#### Scenario: 路由侧与调用侧共享熔断器

- **WHEN** `HealthRouter` 过滤熔断渠道
- **THEN** 系统 SHALL 用 `channelId + protocol` 派生 `endpointId` 查 `ChannelEndpointCircuitBreakerManager`
- **THEN** 该 manager SHALL 与 `KeyFailoverInvoker` 共享同一 bean 实例

#### Scenario: Health 先于 Priority 使次优先级渠道可被选

- **WHEN** 主优先级渠道熔断，次优先级渠道健康
- **THEN** `HealthRouter` SHALL 先过滤掉熔断渠道
- **THEN** `PriorityRouter` SHALL 在存活渠道里按 priority 排序输出完整列表
- **THEN** 次优先级健康渠道 SHALL 保留在候选列表中成为转移候选
