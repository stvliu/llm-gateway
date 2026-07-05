## Why

当前容灾模型中，Cluster（故障域聚合根）与 Application 在「控制路由走向」上职责交叉：Application 通过 `ApplicationChannel.priority` 配置转移顺序，Cluster 通过 `Channel.clusterId` 驱动共因跳过，两者独立配置、彼此不可见，语义会冲突（应用配的顺序可能被共因跳过覆盖）。且 Cluster 共因跳过会误杀不共因的候选（如同供应商不同账户的多 Key，账户额度独立、不共因，却被同 clusterId 跳过）。经场景验证（研发自动化同供应商多 Key、OpenAI 官方+Azure 跨供应商共因），共因跳过的收益（首次故障省几次失败尝试）配不上其复杂度，且熔断器已覆盖持续故障的痛感。

同时，不同下游应用场景（流程自动化/研发自动化/AGI/BI）对失败处理的诉求差异大（BI 愿快速失败省成本、流程自动化要转移保可用、研发自动化要同渠道换 Key），当前无应用级失败处理策略，无法表达场景差异。

故删除 Cluster 与共因跳过，引入轻量应用级失败处理策略（三选一），容灾完全由 Application 承担。

## What Changes

**删除（减法）**：
- **BREAKING** 删除 Cluster 故障域聚合根全套：实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- **BREAKING** 删除 `Channel.clusterId` 字段、`RoutingContext.clusterId` 字段
- **BREAKING** 删除 `ChannelFailoverInvoker` 共因跳过逻辑（`commonCauseFailedClusters` + 跳过判定 + `publishFailoverEvent` 的 `commonCauseSkip` 参数）
- **BREAKING** 删除 `FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段
- **BREAKING** 删除 `FailoverEventGateway.findRecent` 的 clusterId 过滤参数
- **BREAKING** Flyway 迁移：删 `clusters` 表、`channels.cluster_id` 列、`failover_events.from_cluster_id`/`to_cluster_id`/`common_cause_skip` 列
- 删除前端 Cluster 拓扑卡片 + 共因跳过展示 + `grouping.ts`
- **BREAKING** `cluster-failover` capability 整体退场

**新增（加法）**：
- 引入应用级失败处理策略枚举（三选一互斥，挂 Application，轻量单字段，不独立实体）：
  - `FAIL_FAST`（快速失败）：L0 不跑、L1 不跑，第一个 Key 失败立即抛错
  - `FAIL_OVER`（失败转移）：L0 跑、L1 跑，同渠道换 Key + 换渠道，全耗尽抛错（当前行为）
  - `FAIL_RETRY`（失败重试）：L0 跑、L1 不跑，同渠道内换 Key，不换渠道
- 默认策略：`FAIL_RETRY`（失败重试）——契合「同供应商多 Key」主场景，K1 限流换 K2
- **BREAKING** 现有应用数据迁移：设为 `FAIL_OVER`（保持原行为不变）
- `ChannelFailoverInvoker` 按应用策略控制 L0/L1 行为

**补齐管理员容灾管理前端功能**：
- 端点熔断应急操作 UI（forceOpen/forceClose + 状态展示）
- 端点熔断状态大盘（容灾总览页）
- 容灾总览页重组：删 Cluster 拓扑后 = 转移事件流 + 耗尽告警 + 端点熔断状态大盘
- 应用失败处理策略配置 UI（ApplicationFormModal 加策略选择）
- 确保 Application 渠道 priority 配置 UI 完整可用

**非目标**：
- 不做共因跳过（无论 Cluster、共因组字段、providerId 判定均不做）
- 不做场景模板（三策略已覆盖场景差异，模板 YAGNI）
- 不做下游应用请求级选择渠道分组
- 不恢复 ResilienceProfile / L2 降级

## Capabilities

### New Capabilities
- `application-failure-strategy`: 应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），控制 L0/L1 故障转移行为

### Modified Capabilities
- `application`: Application 实体新增失败处理策略字段
- `channel-failover`: 删除 L1 共因跳过 requirement 与转移事件 clusterId/commonCauseSkip 字段，L1 转移改为按 ApplicationChannel.priority 顺序 + 应用策略控制 L0/L1
- `resilience-console`: 删除 Cluster 拓扑展示与共因跳过列，新增端点熔断应急操作 UI 与端点熔断状态大盘，应用管理页加失败处理策略配置，容灾总览页重组

## Impact

- **后端代码**：`domain/resilience`（Cluster 整删、FailoverEvent 瘦身）、`domain/application`（Application 加策略字段）、`domain/supply`（Channel 删 clusterId、RoutingContext 删 clusterId）、`application/proxy`（ChannelFailoverInvoker 按策略控制 L0/L1、删共因跳过、RoutingResolver 删 clusterId 填充）、`application/resilience`（ClusterService 整删）、`adapter/api`（ClusterController 整删、ApplicationController 策略字段）、`infrastructure/resilience`（Cluster Gateway 整删）
- **DB schema**：删 clusters 表、channels.cluster_id 列、failover_events 相关列；applications 表加策略字段；数据迁移现有应用策略=FAIL_OVER
- **前端**：gateway-console 容灾总览页重组、应用策略配置 UI、端点熔断 UI 新增、Cluster 相关清除
- **API**：**BREAKING** `/api/v1/resilience/clusters` 整删；转移事件查询响应字段调整；Application 创建/更新/查询含策略字段
- **依赖**：复用既有 `KeyFailoverInvoker`（L0）、`ChannelEndpointCircuitBreakerManager`（熔断应急）
- **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为保持
- **回头路警示**：应用级策略仅为 Application 上单枚举字段，不得演变为已删的 ResilienceProfile（独立实体+全局解析链+L2/PinnedModel/会话亲和）
