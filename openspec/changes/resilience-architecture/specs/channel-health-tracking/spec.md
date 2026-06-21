# Channel Health Tracking Delta Spec

## MODIFIED Requirements

### Requirement: 熔断 key 统一为 endpointId

熔断 key SHALL 统一为 `endpointId`，路由侧（`HealthRouter`）与调用侧（`KeyFailoverInvoker`）共享同一熔断器实例（`ChannelEndpointCircuitBreakerManager`）。

**变更要点**:
- 原 `HealthRouter` 用 `channelId` 查熔断，`KeyFailoverInvoker` 用 `endpointId`，路由侧与调用侧熔断互不可见
- 现 `HealthRouter`（`@Order(200)`）用 `channelId + protocol` 经 `EndpointResolver` 派生 `endpointId` 后查 `ChannelEndpointCircuitBreakerManager`
- 与 `KeyFailoverInvoker`（用 `RoutingContext.channelEndpointId()`）共享同一 manager bean
- 不向 `ModelInstance`/`model_instances` 表加 `endpointId` 字段（channel 粒度与 channel×protocol 端点粒度 1:1 不自洽，采用运行时派生方案）

**RouterChain 顺序修正**:
- 原 `Permission(@100) → Priority(@200) → Health(@300) → LoadBalance(@9999)`（Priority 先于 Health 且 force，导致次优先级渠道永不被选）
- 现 `Permission(@100) → Health(@200) → ClusterAffinity(@250) → Priority(@300) → PinnedModel(@350) → LoadBalance(@9999)`（Health 先于 Priority，次优先级健康渠道能成为转移候选）

#### Scenario: 路由侧与调用侧共享熔断器

- **WHEN** `HealthRouter` 过滤熔断渠道
- **THEN** 系统 SHALL 用 `channelId + protocol` 派生 `endpointId` 查 `ChannelEndpointCircuitBreakerManager`
- **THEN** 该 manager SHALL 与 `KeyFailoverInvoker` 共享同一 bean 实例

#### Scenario: Health 先于 Priority 使次优先级渠道可被选

- **WHEN** 主优先级渠道熔断，次优先级渠道健康
- **THEN** `HealthRouter` SHALL 先过滤掉熔断渠道
- **THEN** `PriorityRouter` SHALL 在存活渠道里按 priority 分组
- **THEN** 次优先级健康渠道 SHALL 成为转移候选

### Requirement: ProviderHealthTracker 收窄为供应商级粗粒度信号

`ProviderHealthTracker` 职责 SHALL 收窄为供应商级粗粒度信号，仅用于 L2 备选模型可用性判断，不再驱动 L1 路由决策。

**变更要点**:
- 原 `ProviderHealthTracker` 驱动 L1 路由决策（与 `HealthRouter` 端点级熔断职责重叠）
- 现 L1 路由决策由端点级 `ChannelEndpointCircuitBreakerManager` 驱动
- `ProviderHealthTracker` 退为供应商级粗粒度信号，供 L2 备选模型可用性参考

#### Scenario: L1 路由由端点级熔断驱动

- **WHEN** L1 路由过滤熔断渠道
- **THEN** 系统 SHALL 使用 `ChannelEndpointCircuitBreakerManager`（端点级）而非 `ProviderHealthTracker`（供应商级）

#### Scenario: ProviderHealthTracker 仅供 L2 参考

- **WHEN** L2 备选模型可用性判断
- **THEN** 系统 MAY 参考 `ProviderHealthTracker` 供应商级粗粒度信号
- **THEN** `ProviderHealthTracker` SHALL NOT 驱动 L1 路由决策
