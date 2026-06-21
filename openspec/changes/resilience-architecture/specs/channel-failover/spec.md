# Channel Failover Delta Spec

## ADDED Requirements

### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

系统 SHALL 提供 `ChannelFailoverInvoker` 作为 Channel 级（L1）运行时失败转移回路，在候选列表内逐个尝试，替代原 `DegradationInvoker` 作为主转移路径。

**四层容灾栈**:
- L0 Key 级（同渠道换 Key，复用 `KeyFailoverInvoker`）
- L1 Channel 级（同模型换渠道，本 capability 核心）
- L2 模型级（换能力近似模型，应用可选兜底）
- L3 抛错

**调用入口**:
- `invoke(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, ResilienceProfile profile)` — 非流式
- `invokeStream(...)` — 流式

#### Scenario: L1 候选内逐个尝试

- **WHEN** `ChannelFailoverInvoker.invoke` 接收按 cluster/priority 排序的候选列表
- **THEN** 系统 SHALL 对每个候选依次调用 `KeyFailoverInvoker.invoke`（内部跑 L0 Key 级转移）
- **THEN** 某候选成功时 SHALL 立即返回响应

#### Scenario: L1 候选失败按错误分流决策

- **WHEN** 某候选调用抛出 `ProviderException`
- **THEN** 系统 SHALL 经 `ErrorClassifier.classify(errorType)` 得到 `FailoverDecision`
- **THEN** 决策为 `NONE` 时 SHALL 直接抛出原异常（不转移）
- **THEN** 决策为 `L1`/`L2` 时 SHALL 发布转移事件后继续尝试下一候选

#### Scenario: L1 全耗尽才进 L2

- **WHEN** 所有 L1 候选均失败
- **THEN** 系统 SHALL 进入 L2 模型降级（`tryL2Degradation`）
- **THEN** L2 门禁未通过或无备选时 SHALL 抛出最后捕获的异常
- **THEN** L2 降级成功时 SHALL 抛出 `L2DegradationRequiredException`（携带 fallbackModel）由上层重路由

### Requirement: 错误分流表

系统 SHALL 提供错误分流表（`ErrorClassifier`），按 `ProviderErrorType` 映射到 `FailoverDecision`（L1/L2/NONE），指导转移层级选择。

**分流规则**:
- `INVALID_REQUEST` → `NONE`（请求级错误，换哪都无效，直接抛出）
- 共因故障（`AUTHENTICATION_ERROR`/`RATE_LIMIT_ERROR`/`QUOTA_EXCEEDED`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）→ `L1`（换渠道）
- `UNKNOWN_ERROR` → `L2`（模型能力问题，换模型降级）
- `null` 输入 → `NONE`（编程错误或未分类，直接抛出不转移）
- 未在表中显式映射的新增枚举值 → `L2`（兜底防御性降级）

#### Scenario: 请求级错误不转移

- **WHEN** 候选调用抛出 `errorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不尝试下一候选

#### Scenario: 共因故障走 L1 换渠道

- **WHEN** 候选调用抛出 `errorType = AUTHENTICATION_ERROR`（或 `QUOTA_EXCEEDED`/`RATE_LIMIT_ERROR`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `ChannelFailoverInvoker` SHALL 发布转移事件后尝试下一候选

#### Scenario: 模型能力问题走 L2 换模型

- **WHEN** 候选调用抛出 `errorType = UNKNOWN_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L2`
- **THEN** L1 候选耗尽后 SHALL 进入 L2 模型降级

### Requirement: 候选列表路由产出

`InstanceSelector.select` SHALL 返回按 (cluster, priority) 排序的候选列表（而非单实例），供 L1 逐个尝试。`LoadBalanceRouter` 降级为透传，不再收敛到单实例。

**规则**:
- 候选列表经 `RouterChain` 过滤链产出：`Permission(@100) → Health(@200) → ClusterAffinity(@250) → Priority(@300) → PinnedModel(@350) → LoadBalance(@9999)`
- Health 先于 Priority（修正原 Priority 先于 Health 导致次优先级渠道永不被选的问题）
- 候选按 `priority` 升序排序

#### Scenario: 返回排序候选列表

- **WHEN** `InstanceSelector.select(modelId, applicationId, userId, role, strategy, protocol)` 被调用
- **THEN** 系统 SHALL 返回按 `priority` 升序的候选 `ModelInstance` 列表
- **THEN** 列表 SHALL 供 `ChannelFailoverInvoker` 逐个尝试
- **THEN** 系统 SHALL NOT 收敛到单实例

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

`ChannelFailoverInvoker` SHALL 在 L1/L2 决策换下一候选前，经 `DomainEventPublisher` 发布 `FailoverOccurredEvent`，由 `FailoverEventListener`（`@EventListener` 同步处理）持久化为 `FailoverEvent` 实体。

**规则**:
- 发布与持久化通过事件解耦：监听器捕获异常仅记日志，不阻断调用链
- 可靠性边界：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，非计费/审计关键路径）
- 事件字段含 `traceId`（串联同请求多次转移）、`fromChannelId/fromEndpointId`、`toChannelId/toEndpointId`、`errorType`、`decision`、`exhausted`、`occurredAt`

#### Scenario: 换候选前发布转移事件

- **WHEN** L1/L2 决策成立且 `ChannelFailoverInvoker` 即将尝试下一候选
- **THEN** 系统 SHALL 发布 `FailoverOccurredEvent`，含 from/to 渠道端点、errorType、decision
- **THEN** `FailoverEventListener` SHALL 持久化为 `FailoverEvent`

#### Scenario: 持久化失败不阻断业务

- **WHEN** `FailoverEvent` 持久化抛出异常
- **THEN** 监听器 SHALL 仅记录日志，不向上抛出
- **THEN** 调用链 SHALL 继续执行转移
