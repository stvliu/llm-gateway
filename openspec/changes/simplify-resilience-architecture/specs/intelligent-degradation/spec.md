# Intelligent Degradation Delta Spec

## REMOVED Requirements

### Requirement: 降级链配置
**Reason**: L2 模型降级层整体移除。降级是「有损的最后手段」，覆盖的模型能力问题（上下文超长/能力缺失/模型缺陷）大多能被请求阶段能力匹配前置消解；降级链人工预配置、有损、静默，与「下游是应用而非人」「Token 成本透明」原则冲突。容灾止于通道（L0/L1），降级决策还给应用。
**Migration**: 应用如需换模型能力，由应用自身决定（不依赖网关运行时降级）。`DegradationService`/`DegradationServiceImpl`/`DegradationProperties`/`DegradationEvent`/`DegradationRecoveredEvent` 整删，`@Scheduled recoveryCheck` 一并删除。

### Requirement: 降级触发
**Reason**: L2 模型降级层移除，降级触发条件不再适用。共因故障由 L1 Channel 级转移处理，请求级错误直接抛出，模型能力问题交给应用。
**Migration**: 见「降级链配置」迁移说明。

### Requirement: 降级通知
**Reason**: L2 降级移除，降级事件（`DegradationEvent`/`DegradationRecoveredEvent`）不再产生。
**Migration**: 事件类整删。容灾可观测性由转移事件流（`FailoverOccurredEvent`）承载。

### Requirement: 自动回切
**Reason**: L2 降级移除，`recoveryCheck` 定时回切不再需要。
**Migration**: `@Scheduled recoveryCheck` 与相关逻辑整删。

### Requirement: Metrics 埋点
**Reason**: L2 降级移除，降级相关 Metrics（`gateway.degradation.*`）不再产生。
**Migration**: 降级 Metrics 整删；转移 Metrics（`gateway.failover.*`）保留。

### Requirement: L2 降级信号由上层重路由
**Reason**: L2 降级移除，`L2DegradationRequiredException` 信号机制不再存在。L1 候选全耗尽后直接抛最后异常，不再进 L2 重路由。
**Migration**: `L2DegradationRequiredException` 整删；`ChatDispatchServiceImpl` 的 `invokeWithL2Failover`/`invokeStreamWithL2Failover`/`resolveMaxDepth`/`unwrapL2Cause`/`MAX_DEGRADATION_DEPTH` 整删，改为直接调用 `ChannelFailoverInvoker`。
