## Why

当前容灾架构（`resilience-architecture` change 引入）存在「看起来智能但无实效」的中间决策层与已存缺陷：L2 模型降级有损静默且覆盖边缘场景、Cluster 域级健康聚合与端点级熔断等价、PriorityRouter 选择器语义导致 L1 换不到备渠道、调谐只对首项做一次导致换渠道后请求错误。经架构探讨，认定应将容灾收敛为「解决实际问题的最小充分集」：容灾止于通道（L0/L1），降级决策还给应用；Cluster 语义改造为跨供应商故障独立性分组、驱动共因跳过，做端点级熔断做不到的预测性转移。

## What Changes

- **BREAKING** 删除 L2 模型降级层：整删 `DegradationService` 包、`L2DegradationRequiredException`、L2 重路由循环、`ResilienceProfile` 的 L2 字段。容灾栈由四层收敛为三层（L0 换 Key / L1 换渠道 / L3 抛错）
- **BREAKING** Cluster 语义改造（保留 `Cluster` 实体名）：语义从「供应商内分组（region/priority/healthStatus 字段膨胀）」改为「跨供应商故障独立性分组」，瘦身字段为极简（code/name/description + 审计，删 region/priority/healthStatus）。`Channel.clusterId` 字段名保留不变。`Cluster` 与 `providerId` 共存正交
- **BREAKING** 转移顺序由全局 `ModelInstance.priority` 改为应用级 `ApplicationChannel.priority`：每应用各自定义候选转移顺序，无主备只有先后次序。`ModelInstance.priority` 退场
- 修复 PriorityRouter 缺陷：从「选择器」（只留 priority 最小组，isForce=true，丢备候选）改为「排序器」（按应用级 priority 升序输出完整候选队列，不收敛）
- 修复调谐缺陷：调谐从 `ChatDispatchServiceImpl` 对首项做一次，下沉到 `ChannelFailoverInvoker` 内每候选独立调谐（基于原始请求派生副本，不携带前一候选痕迹）
- 新增 L1 共因跳过：`ChannelFailoverInvoker` 转移时，当前候选共因失败 → 跳过同 `clusterId` 后续候选 → 试异域候选（本次请求内有效，不持久化）
- 删除 DomainHealth 路由器：整删 `ClusterHealthAggregator` 与 `ClusterAffinityRouter`，域级预判与端点级熔断等价
- 删除 PinnedModel：整删 `PinnedModelRouter` 与画像 `enablePinnedModel`/`pinnedModelId` 字段（模型锁定语义冗余，授权已覆盖）
- 删除会话亲和：整删 `SessionAffinityStore`（Redis/InMemory 双实现）与画像字段（LLM 多无状态，延后至有上游缓存收益）
- `ResilienceProfile` 实体降级：删 L2/PinnedModel/会话亲和后只剩 `timeout`，直接挂 `Application` 字段，不独立成实体
- 错误分流表调整：`UNKNOWN_ERROR` 由 L2 改为 NONE（删 L2 后未分类错误不转移直接抛）；`FailoverDecision` 收敛为 L1/NONE
- 转移事件流调整：`FailoverOccurredEvent`/`FailoverEvent` 保留 clusterId 字段（语义随 Cluster 改造），新增「是否共因跳过」标记

## Capabilities

### New Capabilities
<!-- 无新增 capability。Cluster 由 cluster-failover 改造而来，非新增 -->

### Modified Capabilities
- `channel-failover`: 容灾栈由四层收敛为三层（删 L2）；L1 改为「先后次序 + clusterId 共因跳过」；候选列表按 `ApplicationChannel.priority` 排序产出；PriorityRouter 从选择器改排序器；调谐下沉每候选独立；错误分流表 UNKNOWN→NONE，FailoverDecision 收敛 L1/NONE；流式首字节边界保留
- `channel-health-tracking`: 熔断 key 仍统一 endpointId；删除 DomainHealth 域级聚合路由器（`ClusterHealthAggregator`/`ClusterAffinityRouter`）；RouterChain 顺序去除 ClusterAffinity 与 PinnedModel
- `cluster-failover`: **BREAKING** Cluster 语义改造为跨供应商故障独立性分组——瘦身字段（删 region/priority/healthStatus），`Channel.clusterId` 保留；删除域级健康聚合 Requirement；新增「Cluster 作 L1 共因跳过依据」Requirement
- `intelligent-degradation`: **BREAKING** 整删——L2 模型降级层移除，`DegradationService` 退场
- `resilience-profile`: **BREAKING** 实体降级——删 L2/PinnedModel/会话亲和字段；`ResilienceProfile` 实体退场，`timeout` 挂 `Application`；解析链 Application→Global 简化为 Application.timeout
- `resilience-console`: 容灾总览页移除降级/会话亲和/PinnedModel 相关；转移事件流保留 clusterId（语义随 Cluster 改造）；画像模板页随 ResilienceProfile 退场调整
- `upstream-exception-classification`: 错误分流表 `UNKNOWN_ERROR` 由 L2 改为 NONE；`FailoverDecision` 收敛为 L1/NONE
- `application-access-control`: `ApplicationChannel` 新增 `priority` 字段（应用级转移顺序）；`Application` 新增 `timeout` 字段（承接 ResilienceProfile 降级）
- `model-instance`: 删除 `ModelInstance.priority`（转移顺序改由 ApplicationChannel.priority 承载）
- `application`: `Application` 新增 `timeout` 字段；移除 `resilienceProfileId` 关联（ResilienceProfile 退场）

## Impact

- **后端代码**：`application/proxy`（routing/invoker）、`application/degradation`（整删）、`application/resilience`、`domain/resilience`、`domain/application`、`domain/supply`、`infrastructure/resilience`、`adapter/api`、`adapter/protocol`
- **DB schema**：clusters 表删 region/priority/health_status 列、application_channels 加 priority 列、删 resilience_profiles 表、failover_events 表 cluster_id 列保留、failover_events 加 common_cause_skip 列。**注**：`model_instances.priority` 列的物理删除（V63）由用户决策拆为独立后续 change，本 change 未执行——运行时转移顺序已完全由 `ApplicationChannel.priority` 驱动，`ModelInstance.priority` 仅作 DB 粗排兜底
- **前端**：gateway-console 容灾总览页、画像模板页（随 ResilienceProfile 退场）、Applications 页（priority 配置）、Channels 页
- **API**：`/api/v1/resilience/profiles` 随 ResilienceProfile 退场调整；`/api/v1/resilience/clusters` 保留（Cluster 实体名不变，字段瘦身）；转移事件查询响应字段调整
- **依赖**：复用 `ResilientUpstreamClient`/`CircuitBreaker`/`KeyFailoverInvoker`，不重写上游客户端与重试/熔断算法
- **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为保持
