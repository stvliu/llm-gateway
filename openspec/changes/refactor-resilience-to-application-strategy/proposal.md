## Why

当前容灾模型中，Cluster（故障域聚合根）与 Application 在「控制路由走向」上职责交叉：Application 通过 `ApplicationChannel.priority` 配置转移顺序，Cluster 通过 `Channel.clusterId` 驱动共因跳过，两者独立配置、彼此不可见，语义会冲突（应用配的顺序可能被共因跳过覆盖）。同时 Cluster 是全局渠道侧概念，无法表达不同下游应用场景（流程自动化/研发自动化/AGI/BI）对容灾的差异化诉求。需将容灾配置收敛到 Application，并引入应用级场景化容灾策略，让管理员能为不同下游场景快速配置差异化容灾。

## What Changes

**删除（减法）**：
- **BREAKING** 删除 Cluster 故障域聚合根全套：实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- **BREAKING** 删除 `Channel.clusterId` 字段、`RoutingContext.clusterId` 字段
- **BREAKING** 删除 `ChannelFailoverInvoker` 共因跳过逻辑（`commonCauseFailedClusters` + 跳过判定 + `publishFailoverEvent` 的 `commonCauseSkip` 参数），保留基础 L1 按 `ApplicationChannel.priority` 顺序转移
- **BREAKING** 删除 `FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段
- **BREAKING** 删除 `FailoverEventGateway.findRecent` 的 clusterId 过滤参数，调用方适配
- **BREAKING** Flyway 迁移：删 `clusters` 表、`channels.cluster_id` 列、`failover_events.from_cluster_id`/`to_cluster_id`/`common_cause_skip` 列
- 删除前端 Cluster 拓扑卡片 + 共因跳过展示 + `grouping.ts`
- **BREAKING** `cluster-failover` capability 整体退场

**新增（加法）**：
- 引入应用级容灾策略（挂在 Application，轻量，不独立成实体），含差异化维度：共因跳过开关（基于 providerId，应用可选）/ 候选耗尽行为 / 成本控制 / 转移触发条件（具体维度与数据模型由 design 阶段 brainstorming 定）
- 引入场景模板（研发自动化/流程自动化/AGI/BI 预设推荐值），管理员可一键套用快速配置
- 前端应用容灾策略配置页 + 模板选择

**补齐管理员前端功能**：
- 端点熔断应急操作 UI（forceOpen/forceClose + 状态展示）
- 端点熔断状态大盘（容灾总览页）
- 容灾总览页重组：删 Cluster 拓扑后 = 转移事件流 + 耗尽告警 + 端点熔断状态
- 确保应用 priority/timeout 配置 UI 完整可用

## Capabilities

### New Capabilities
- `application-resilience-strategy`: 应用级场景化容灾策略——共因跳过开关、候选耗尽行为、成本控制、转移触发条件等差异化维度，及场景模板（研发自动化/流程自动化/AGI/BI）

### Modified Capabilities
- `application`: Application 实体挂载容灾策略（具体挂载方式由 design 定），保留 timeout
- `channel-failover`: 删除 L1 共因跳过 requirement 与转移事件 clusterId/commonCauseSkip 字段，L1 转移改为纯按 ApplicationChannel.priority 顺序 + 应用级策略驱动
- `resilience-console`: 删除 Cluster 拓扑展示，新增应用容灾策略配置页 + 端点熔断应急操作 UI + 端点熔断状态大盘，容灾总览页重组
- `application-access-control`: ApplicationChannel.priority 配置 UI 完整性确认（无 spec 级变更则不列入，待 design 确认）

## Impact

- **后端代码**：`domain/resilience`（Cluster 整删、FailoverEvent 瘦身）、`domain/application`（Application 挂策略）、`domain/supply`（Channel 删 clusterId、RoutingContext 删 clusterId）、`application/proxy`（ChannelFailoverInvoker 删共因跳过、RoutingResolver 删 clusterId 填充）、`application/resilience`（ClusterService 整删、策略 Service 新增）、`adapter/api`（ClusterController 整删、ApplicationController 策略端点、ChannelController 熔断 UI 无后端变更）、`infrastructure/resilience`（Cluster Gateway 整删）
- **DB schema**：删 clusters 表、channels.cluster_id 列、failover_events 相关列；新增应用级策略字段/表（由 design 定）
- **前端**：gateway-console 容灾总览页重组、应用策略配置页新增、端点熔断 UI 新增、Cluster 相关清除
- **API**：**BREAKING** `/api/v1/resilience/clusters` 整删；转移事件查询响应字段调整（删 clusterId/commonCauseSkip）；新增应用策略配置端点
- **依赖**：复用既有 `ResilientUpstreamClient`/`CircuitBreaker`/`KeyFailoverInvoker`/`ChannelEndpointCircuitBreakerManager`
- **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为保持
- **回头路警示**：应用级策略不得演变为已删的 ResilienceProfile（独立实体+全局解析链+L2/PinnedModel/会话亲和）；「候选耗尽降级」仅指应用预配兜底模型，非网关自动降级链
