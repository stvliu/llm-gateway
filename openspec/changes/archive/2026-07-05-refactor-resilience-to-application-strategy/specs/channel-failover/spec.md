# Channel Failover Delta Spec

## MODIFIED Requirements

### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

系统 SHALL 提供 `ChannelFailoverInvoker` 作为 Channel 级（L1）运行时失败转移回路，按候选所属应用的 `failureStrategy` 控制 L0/L1 行为，作为主转移路径。

**三层容灾栈**（删除 L2 模型级 + 删除 clusterId 共因跳过）:
- L0 Key 级（同渠道换 Key，复用 `KeyFailoverInvoker`）
- L1 Channel 级（同模型按 ApplicationChannel.priority 顺序换渠道）
- L3 抛错

**调用入口**:
- `invoke(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId)` — 非流式
- `invokeStream(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId, StreamCallback callback)` — 流式

**每候选独立调谐**：试每个候选前 SHALL 基于原始请求派生副本，用该候选的 `RoutingContext` 独立调谐，不携带前一候选痕迹。

**策略驱动的 L0/L1 行为**（详见 application-failure-strategy capability）:
- `FAIL_FAST`：候选首个 Key 失败立即抛错，L0/L1 均不跑
- `FAIL_RETRY`（默认）：L0 跑（同渠道换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错
- `FAIL_OVER`：L0 跑 + L1 跑（按 priority 换渠道），全耗尽抛错

**故障跳过机制**：L1 不做共因跳过。由端点级熔断器（`ChannelEndpointCircuitBreakerManager`）在端点连续失败 OPEN 后跳过该端点，不引入共因分组。

#### Scenario: L1 候选内逐个尝试（FAIL_OVER 策略）

- **WHEN** 应用 `failureStrategy=FAIL_OVER`，`ChannelFailoverInvoker.invoke` 接收按 ApplicationChannel.priority 排序的候选列表
- **THEN** 系统 SHALL 对每个候选依次调用 `KeyFailoverInvoker.invoke`（L0 Key 级转移）
- **THEN** 某候选成功时 SHALL 立即返回响应

#### Scenario: L1 候选失败按错误分流决策

- **WHEN** 某候选调用抛出 `ProviderException`
- **THEN** 系统 SHALL 经 `ErrorClassifier.classify(errorType)` 得到 `FailoverDecision`
- **THEN** 决策为 `NONE` 时 SHALL 直接抛出原异常（不转移）
- **THEN** 决策为 `L1` 时 SHALL 按策略处置（FAIL_OVER 继续下一候选；FAIL_RETRY 同渠道换 Key；FAIL_FAST 抛错）

#### Scenario: L1 全耗尽抛最后异常

- **WHEN** 所有 L1 候选均失败（FAIL_OVER 策略下）
- **THEN** 系统 SHALL 抛出最后捕获的异常
- **THEN** 系统 SHALL NOT 调用任何降级服务

### Requirement: 转移事件发布

`ChannelFailoverInvoker` SHALL 在 L1 换候选前（FAIL_OVER 策略下）发布 `FailoverOccurredEvent`，记录转移事件用于容灾可观测。事件字段 SHALL 包含 `traceId`/`applicationId`/`fromChannelId`/`fromEndpointId`/`toChannelId`/`toEndpointId`/`errorType`/`decision`/`exhausted`/`occurredAt`。**删除** `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段（Cluster 与共因跳过退场）。

#### Scenario: 转移事件不含 clusterId 与 commonCauseSkip

- **WHEN** 候选转移发生
- **THEN** 事件 SHALL 不包含 fromClusterId/toClusterId/commonCauseSkip 字段

## REMOVED Requirements

### Requirement: L1 共因跳过
**Reason**: Cluster 故障域根实体退场，基于 clusterId 的共因跳过机制随之移除。共因跳过误杀不共因候选（如同供应商不同账户的多 Key，账户额度独立、故障不共因，却被同 clusterId 跳过），且与 Application 在路由走向上职责交叉。故障跳过改由端点级熔断器（连续失败 OPEN 后跳过）+ 应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 控制 L0/L1）承担，不引入共因分组。
**Migration**: `ChannelFailoverInvoker` 删除 `commonCauseFailedClusters` 局部 Set 与跳过判定逻辑，L0/L1 行为改为按应用 `failureStrategy` 控制。`RoutingContext.clusterId`、`FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段删除，`FailoverEventGateway.findRecent` 的 clusterId 过滤参数删除。
