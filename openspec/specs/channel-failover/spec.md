# channel-failover Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
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

**故障跳过机制**：L1 不做共因跳过。由端点级熔断器（`ChannelEndpointCircuitBreakerService`）在端点连续失败 OPEN 后跳过该端点，不引入共因分组。

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

### Requirement: 错误分流表

系统 SHALL 提供错误分流表（`ErrorClassifier`），按 `ProviderErrorType` 映射到 `FailoverDecision`（L1/NONE），指导转移决策。

**分流规则**（删除 L2，UNKNOWN 改 NONE）:
- `INVALID_REQUEST` → `NONE`（请求级错误，换哪都无效，直接抛出）
- 共因故障（`AUTHENTICATION_ERROR`/`RATE_LIMIT_ERROR`/`QUOTA_EXCEEDED`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）→ `L1`（换渠道）
- `UNKNOWN_ERROR` → `NONE`（未分类错误，不转移直接抛，降级决策还给应用）
- `null` 输入 → `NONE`（编程错误或未分类，直接抛出不转移）
- 未在表中显式映射的新增枚举值 → `NONE`（兜底不转移，直接抛）

**FailoverDecision 枚举收敛为 L1/NONE**（删除 L2）。

#### Scenario: 请求级错误不转移

- **WHEN** 候选调用抛出 `errorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不尝试下一候选

#### Scenario: 共因故障走 L1 换渠道

- **WHEN** 候选调用抛出 `errorType = AUTHENTICATION_ERROR`（或 `QUOTA_EXCEEDED`/`RATE_LIMIT_ERROR`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `ChannelFailoverInvoker` SHALL 发布转移事件后尝试下一候选

#### Scenario: 未分类错误不转移

- **WHEN** 候选调用抛出 `errorType = UNKNOWN_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移

### Requirement: 候选列表路由产出

`InstanceSelector.select` SHALL 返回按 ApplicationChannel.priority 排序的候选列表（而非单实例），供 L1 逐个尝试。`LoadBalanceRouter` 降级为透传，不再收敛到单实例。

**规则**（删除 ClusterAffinity 与 PinnedModel，Priority 改排序器）:
- 候选列表经 `RouterChain` 过滤链产出：`Permission(@100) → EndpointHealth(@200) → Priority(@300) → LoadBalance(@9999)`
- `PriorityRouter` 为排序器：按应用级 `ApplicationChannel.priority` 升序输出完整候选列表，SHALL NOT 收敛到最优组（修复原选择器 isForce=true 导致备候选被丢、L1 换不到备的缺陷）
- 候选按 `ApplicationChannel.priority` 升序排序（应用级优先，无主备只有先后次序）

#### Scenario: 返回排序候选列表

- **WHEN** `InstanceSelector.select(modelId, applicationId, userId, role, strategy, protocol)` 被调用
- **THEN** 系统 SHALL 返回按 `ApplicationChannel.priority` 升序的候选 `ModelInstance` 列表
- **THEN** 列表 SHALL 供 `ChannelFailoverInvoker` 逐个尝试
- **THEN** 系统 SHALL NOT 收敛到单实例或最优组

#### Scenario: 主备 priority 不同时备候选保留

- **WHEN** 候选含主渠道（priority=1）与备渠道（priority=2）
- **THEN** `PriorityRouter` SHALL 输出完整列表 [主, 备]，不丢弃备
- **THEN** 主渠道失败时 L1 SHALL 能换到备渠道

#### Scenario: 无可用实例抛 ResourceNotFoundException

- **WHEN** 路由链过滤后候选列表为空
- **THEN** 系统 SHALL 抛出 `ResourceNotFoundException`

### Requirement: 流式转移边界

流式调用 SHALL 只在首字节前转移，首字节后失败不换渠道（继承现有约束）。

#### Scenario: 首字节前可转移

- **WHEN** 流式调用在首字节前某候选失败
- **THEN** 系统 SHALL 按错误分流决策尝试下一候选

#### Scenario: 首字节后不换渠道

- **WHEN** 流式调用已收到首字节后失败
- **THEN** 系统 SHALL NOT 换渠道，直接抛出错误

### Requirement: 转移事件发布

`ChannelFailoverInvoker` SHALL 在 L1 换候选前（FAIL_OVER 策略下）发布 `FailoverOccurredEvent`，记录转移事件用于容灾可观测。事件字段 SHALL 包含 `traceId`/`applicationId`/`fromChannelId`/`fromEndpointId`/`toChannelId`/`toEndpointId`/`errorType`/`decision`/`exhausted`/`occurredAt`。**删除** `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段（Cluster 与共因跳过退场）。

#### Scenario: 转移事件不含 clusterId 与 commonCauseSkip

- **WHEN** 候选转移发生
- **THEN** 事件 SHALL 不包含 fromClusterId/toClusterId/commonCauseSkip 字段

