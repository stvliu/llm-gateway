## Why

当前 InstanceSelector 仅按 priority 取第一个 ModelInstance，weight 字段已定义但未使用；ChatDispatchServiceImpl 中 Key 故障转移和降级编排散布在主流程代码中，流式调用缺少 Key 故障转移能力。参照 Dubbo 的 LoadBalance 和 Cluster 分层设计，引入负载均衡和层次化 Invoker 编排，提升网关的流量调度能力和代码可维护性。

## What Changes

- **LoadBalance 接口 + 实现**：参照 Dubbo 的 LoadBalance 设计，新增接口和加权随机/轮询实现，InstanceSelector 按 priority 分组后组内走负载均衡
- **EndpointMetrics 统计基础设施**：为每个端点维护 active/total/duration/failed 统计，为 LeastActive 等策略提供数据基础
- **健康感知路由**：InstanceSelector 跳过 CircuitBreaker OPEN 状态的端点
- **Invoker 分层重构**：从 ChatDispatchServiceImpl 提取 KeyFailoverInvoker（Key 级故障转移）和 DegradationInvoker（模型降级），形成层次化调用链
- **流式 Key 故障转移**：补齐流式调用的 Key 遍历能力，与非流式对齐

## Capabilities

### New Capabilities
- `load-balance`: 负载均衡策略（加权随机、加权轮询、最少活跃优先），通过 LoadBalance 接口可扩展
- `endpoint-metrics`: 端点级调用统计（活跃数、耗时、成功率），供负载均衡和可观测性使用
- `invoker-chain`: 层次化 Invoker 调用链（DegradationInvoker → KeyFailoverInvoker → ResilientUpstreamClient）

### Modified Capabilities
- `<empty>`: 无 spec 级需求变更

## Impact

- `application/proxy/routing/InstanceSelector.java`：重构 select 方法，引入 LoadBalance
- `application/proxy/routing/RoutingResolver.java`：传递 strategy 参数
- `application/proxy/ChatDispatchServiceImpl.java`：提取 KeyFailover 和 Degradation 到独立 Invoker
- `infrastructure/resilience/`：新增 EndpointMetrics 和 EndpointMetricsRegistry
- `infrastructure/resilience/ResilientUpstreamClient.java`：增加 EndpointMetrics 埋点
- `application/proxy/invoker/`：新增包，放置 KeyFailoverInvoker + DegradationInvoker
