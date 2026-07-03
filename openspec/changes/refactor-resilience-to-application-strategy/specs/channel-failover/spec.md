# Channel Failover Delta Spec

## REMOVED Requirements

### Requirement: L1 共因跳过
**Reason**: Cluster 故障域聚合根退场，基于 clusterId 的全局共因跳过机制随之移除。共因处理收敛到应用级容灾策略（基于 providerId 的应用可选开关），消除 Cluster 与 Application 在路由走向上的职责交叉。
**Migration**: `ChannelFailoverInvoker` 删除 `commonCauseFailedClusters` 局部 Set 与跳过判定逻辑，L1 转移改为纯按 `ApplicationChannel.priority` 顺序逐候选尝试。共因跳过能力由应用级策略（application-resilience-strategy capability）按 providerId 重新提供。`RoutingContext.clusterId`、`FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段删除，`FailoverEventGateway.findRecent` 的 clusterId 过滤参数删除。

## MODIFIED Requirements

### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

`ChannelFailoverInvoker` SHALL 在 L1 转移时按 `ApplicationChannel.priority` 顺序逐候选尝试，候选共因失败（`FailoverDecision=L1`）时记录并继续下一候选，全部耗尽抛最后异常或按应用级容灾策略的耗尽行为处置。共因跳过（同 providerId 跳过）由应用级策略开关控制，开启时按 `Channel.providerId` 判定共因。

#### Scenario: 按应用 priority 顺序逐候选转移

- **WHEN** 候选1 共因失败（L1），应用策略关闭共因跳过
- **THEN** `ChannelFailoverInvoker` SHALL 按顺序试下一候选
- **THEN** 全部耗尽时按策略耗尽行为处置（抛错或降级兜底）

#### Scenario: 应用策略开启共因跳过

- **WHEN** 候选1（providerId=A）共因失败，应用策略开启共因跳过
- **THEN** `ChannelFailoverInvoker` SHALL 跳过同 providerId=A 的后续候选
- **THEN** 系统 SHALL 继续尝试异 providerId 候选

### Requirement: 转移事件发布

`ChannelFailoverInvoker` SHALL 在 L1 换候选前发布 `FailoverOccurredEvent`，记录转移事件用于容灾可观测。事件字段 SHALL 包含 `traceId`/`applicationId`/`fromChannelId`/`fromEndpointId`/`toChannelId`/`toEndpointId`/`errorType`/`decision`/`exhausted`/`occurredAt`。**删除** `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段（Cluster 退场）。

#### Scenario: 转移事件不含 clusterId 与 commonCauseSkip

- **WHEN** 候选转移发生
- **THEN** 事件 SHALL 不包含 fromClusterId/toClusterId/commonCauseSkip 字段
