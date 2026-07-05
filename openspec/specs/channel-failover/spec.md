# channel-failover Specification

## Purpose
TBD - created by archiving change resilience-architecture. Update Purpose after archive.
## Requirements
### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

系统 SHALL 提供 `ChannelFailoverInvoker` 作为 Channel 级（L1）运行时失败转移回路，在候选列表内逐个尝试，作为主转移路径。L0/L1 行为由应用 `failureStrategy` 控制（见 `application` capability）。

**三层容灾栈**（删除 L2 模型级，L0/L1 由应用策略控制）:
- L0 Key 级（同渠道换 Key，复用 `KeyFailoverInvoker`；`FAIL_FAST` 不跑、`FAIL_RETRY`/`FAIL_OVER` 跑）
- L1 Channel 级（同模型换渠道，本 capability 核心；仅 `FAIL_OVER` 跑）
- L3 抛错

**调用入口**（移除 profile 参数，L2 门禁随 L2 删除）:
- `invoke(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId)` — 非流式
- `invokeStream(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId, StreamCallback callback)` — 流式

**每候选独立调谐**（修复原只对首项调谐一次的缺陷）：试每个候选前 SHALL 基于原始请求派生副本，用该候选的 `RoutingContext` 独立调谐（通道级模型名替换按该候选 `upstreamModelName`），不携带前一候选痕迹。

**策略读取**：所有候选同应用同策略，从主候选 `RoutingContext.failureStrategy()` 读取；null 回退 `FAIL_RETRY`（默认）。

**不做共因跳过**：故障时不按 clusterId/providerId 跳过候选。共因渠道（如 OpenAI 官方+Azure）首次故障多试一次，由端点级熔断器在连续失败后 OPEN 跳过。避免误杀不共因候选（同供应商不同账户 Key）。Cluster 故障域实体与 clusterId 字段已删除。

#### Scenario: L1 候选内逐个尝试

- **WHEN** `ChannelFailoverInvoker.invoke` 接收按 ApplicationChannel.priority 排序的候选列表
- **THEN** 系统 SHALL 对每个候选按应用 `failureStrategy` 调用 `KeyFailoverInvoker`（内部跑 L0 Key 级转移）
- **THEN** 某候选成功时 SHALL 立即返回响应

#### Scenario: L1 候选失败按错误分流决策

- **WHEN** 某候选调用抛出 `ProviderException`
- **THEN** 系统 SHALL 经 `ErrorClassifier.classify(errorType)` 得到 `FailoverDecision`
- **THEN** 决策为 `NONE` 时 SHALL 直接抛出原异常（不转移，无视策略）
- **THEN** 决策为 `L1` 时 SHALL 按应用 `failureStrategy` 决定后续行为（`FAIL_FAST` 抛错、`FAIL_RETRY` 同渠道换 Key 不换候选、`FAIL_OVER` 换下一候选）

#### Scenario: L1 全耗尽抛最后异常（删除 L2）

- **WHEN** 所有 L1 候选均失败（`FAIL_OVER` 全试完 / `FAIL_RETRY` 同渠道 Key 耗尽）
- **THEN** 系统 SHALL 抛出最后捕获的异常（不再进入 L2 模型降级）
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

`ChannelFailoverInvoker` SHALL 在 `FAIL_OVER` 策略下 L1 决策换下一候选前，经 `DomainEventPublisher` 发布 `FailoverOccurredEvent`，由 `FailoverEventListener`（`@EventListener` 同步处理）持久化为 `FailoverEvent` 实体。`FAIL_FAST`/`FAIL_RETRY` 不换候选，SHALL NOT 发布转移事件。

**规则**:
- 发布与持久化通过事件解耦：监听器捕获异常仅记日志，不阻断调用链
- 「同步处理」含义：项目未配置 `@EnableAsync`，Spring 事件发布在同一线程内立即调用监听器完成持久化；故发布即持久化，调用链在持久化完成后才继续
- 可靠性边界：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，非计费/审计关键路径）
- 事件字段含 `traceId`（串联同请求多次转移）、`applicationId`、`fromChannelId/fromEndpointId`、`toChannelId/toEndpointId`（exhausted 时为 null）、`errorType`、`decision`（`L1`/`NONE`，L2 已删）、`exhausted`（候选是否全部耗尽）、`occurredAt`
- **删除字段**：`fromClusterId`/`toClusterId`/`commonCauseSkip`（Cluster 故障域与共因跳过已删除）

#### Scenario: 换候选前发布转移事件

- **WHEN** `FAIL_OVER` 策略下 L1 决策成立且 `ChannelFailoverInvoker` 即将尝试下一候选
- **THEN** 系统 SHALL 发布 `FailoverOccurredEvent`，含 from/to 渠道端点、errorType、decision
- **THEN** `FailoverEventListener` SHALL 持久化为 `FailoverEvent`

#### Scenario: 持久化失败不阻断业务

- **WHEN** `FailoverEvent` 持久化抛出异常
- **THEN** 监听器 SHALL 仅记录日志，不向上抛出
- **THEN** 调用链 SHALL 继续执行转移

#### Scenario: FAIL_RETRY 不发布转移事件

- **WHEN** 应用 `failureStrategy = FAIL_RETRY`，同渠道 Key 耗尽
- **THEN** `ChannelFailoverInvoker` SHALL NOT 发布 `FailoverOccurredEvent`（未发生跨候选转移，避免画出未发生的 from→to 路径误导可观测性）
- **THEN** 系统 SHALL 直接抛出最后捕获的异常

