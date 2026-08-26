# Cluster Failover Delta Spec

## ADDED Requirements

### Requirement: Cluster 故障域实体

系统 SHALL 提供显式 `Cluster` 实体作为 Channel 的故障域分组。同组 Channel 共享共因特征（同供应商/同账号/同区域/同专线）。

**实体字段**:
- `code` — 故障域编码，全局唯一（如 `openai-us`/`claude-bedrock`）
- `name` — 故障域名称
- `providerId` — 归属供应商 ID（物理 ID，无 FK 约束）
- `region` — 区域标识（如 `us-east`/`sg`，用于就近路由）
- `priority` — 优先级（数值越小越优先，用于跨域转移排序）
- `healthStatus` — 域级健康聚合状态（`ClusterHealthStatus`：`HEALTHY`/`DEGRADED`/`DOWN`）
- 审计字段继承自 `BaseEntity`

**关联**: `Channel.cluster_id` FK 指向 `clusters.id`（直接建实体，不经软字段阶段）。

**API**:
- `POST /api/v1/resilience/clusters` — 创建故障域（HTTP 201）
- `PUT /api/v1/resilience/clusters/{id}` — 更新故障域
- `GET /api/v1/resilience/clusters/{id}` — 查询故障域详情
- `GET /api/v1/resilience/clusters` — 查询全部故障域列表

**规则**:
- 不提供 delete：Cluster 关联 Channel 的 clusterId，删除需级联清理；且 Gateway 无 delete 方法

#### Scenario: 创建故障域

- **WHEN** 管理员调用 `POST /api/v1/resilience/clusters` 传入合法字段
- **THEN** 系统 SHALL 创建 `Cluster` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201

#### Scenario: Channel 关联故障域

- **WHEN** Channel 配置 `clusterId` 指向某 Cluster
- **THEN** 该 Channel SHALL 归属该故障域
- **THEN** 域级健康聚合 SHALL 纳入该 Channel 的端点熔断状态

### Requirement: 域级健康聚合

`ClusterHealthAggregator` SHALL 依据一个故障域内各端点的熔断状态，聚合出域级健康状态（`ClusterHealthStatus`）。

**聚合规则**:
- 域内全部端点 `CLOSED`（正常）→ `HEALTHY`
- 域内全部端点 `OPEN`（熔断）→ `DOWN`（共因故障，整域不可用）
- 域内部分端点 `OPEN` → `DEGRADED`（容量受损但仍可用）
- 域内任一端点 `HALF_OPEN`（试探放行）→ 视为正在恢复，不判 `DOWN`（仍有 `OPEN` 则 `DEGRADED`，否则 `HEALTHY`）
- 空端点集合 → 保守返回 `HEALTHY`（无故障证据，避免误杀空域）

**规则**:
- 只读查询：通过 `CircuitBreaker.getState()` 读取熔断状态，不调用 `isAvailable`（避免触发 `OPEN→HALF_OPEN` 状态迁移副作用）
- 纯计算组件，不持久化 `Cluster.healthStatus`；路由侧实时聚合判断域是否 DOWN

#### Scenario: 全部端点 CLOSED 聚合为 HEALTHY

- **WHEN** 域内所有端点熔断器状态为 `CLOSED`
- **THEN** `ClusterHealthAggregator.aggregate` SHALL 返回 `HEALTHY`

#### Scenario: 全部端点 OPEN 聚合为 DOWN

- **WHEN** 域内所有端点熔断器状态为 `OPEN`
- **THEN** `ClusterHealthAggregator.aggregate` SHALL 返回 `DOWN`

#### Scenario: 部分端点 OPEN 聚合为 DEGRADED

- **WHEN** 域内部分端点 `OPEN`、部分 `CLOSED`
- **THEN** `ClusterHealthAggregator.aggregate` SHALL 返回 `DEGRADED`

#### Scenario: 任一端点 HALF_OPEN 不判 DOWN

- **WHEN** 域内任一端点熔断器状态为 `HALF_OPEN`
- **THEN** `ClusterHealthAggregator.aggregate` SHALL NOT 返回 `DOWN`
- **THEN** 仍有 `OPEN` 端点时返回 `DEGRADED`，否则返回 `HEALTHY`

### Requirement: ClusterAffinityRouter 故障域亲和路由

`ClusterAffinityRouter`（`@Order(250)`，`isForce=false`）SHALL 排在 `HealthRouter`（`@Order(200)`）之后、`PriorityRouter`（`@Order(300)`）之前，在存活候选中按域聚合判断——域内全部端点熔断（Cluster DOWN）则过滤整域实例，强制跨域转移。

**流程**:
1. 收集候选实例的 `channelId`，通过 `ChannelGateway.findByIds` 批量取 `Channel.clusterId`
2. 按 `clusterId` 分组，每组通过 `EndpointResolver` 按入站协议派生 `endpointId`
3. 调 `ClusterHealthAggregator.aggregate` 聚合域级健康状态
4. 过滤 `DOWN` 域的实例；`DEGRADED`/`HEALTHY` 域保留

**边界处理**:
- `protocol` 为 null：无法派生 endpointId，保守保留全部实例（不误杀）
- 实例 channel 未关联 cluster（`clusterId` null）：不参与域聚合，保守保留
- endpoint 派生失败：该域保守保留（不判 DOWN）
- `isForce=false`：DOWN 域过滤后若候选为空，让链继续而非终止，保留后续 Router 兜底机会

**规则**:
- 容灾转移规则：故障域内优先 → 整域故障才跨域
- 就近路由（按 region 偏好择域）依赖请求级 region 上下文，当前未实现就近排序，仅实现 DOWN 域过滤核心语义

#### Scenario: DOWN 域过滤触发跨域转移

- **WHEN** 某 Cluster 域级健康聚合为 `DOWN`
- **THEN** `ClusterAffinityRouter` SHALL 过滤该域的所有实例
- **THEN** 路由 SHALL 跨域选择其他存活域的实例

#### Scenario: DEGRADED 域保留

- **WHEN** 某 Cluster 域级健康聚合为 `DEGRADED`
- **THEN** `ClusterAffinityRouter` SHALL 保留该域实例（容量受损但仍可承接流量）

#### Scenario: 无 cluster 关联的实例保守保留

- **WHEN** 实例的 `channel.clusterId` 为 null
- **THEN** `ClusterAffinityRouter` SHALL 保留该实例（不参与域聚合）

#### Scenario: DOWN 域过滤后候选为空不终止链

- **WHEN** 所有域均为 `DOWN`，过滤后候选为空
- **THEN** `ClusterAffinityRouter` SHALL 返回空列表但不终止链（`isForce=false`）
- **THEN** 链 SHALL 继续执行后续 Router 寻求兜底
