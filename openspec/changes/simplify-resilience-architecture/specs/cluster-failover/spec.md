# Cluster Failover Delta Spec

> Capability 语义改造：`Cluster` 实体名保留，语义从「供应商内分组（region/priority/healthStatus 膨胀）」改为「跨供应商故障独立性分组」，瘦身字段。`Channel.clusterId` 字段名保留不变。

## REMOVED Requirements

### Requirement: 域级健康聚合
**Reason**: 域级健康聚合（`ClusterHealthAggregator`）在整域端点全 OPEN 时与端点级 HealthRouter 输出等价，价值有限。整域故障靠端点级熔断自然收敛 + L1 转移阶段 clusterId 共因跳过处理。Cluster 仅用于转移阶段共因跳过，不配域级聚合路由器。
**Migration**: `ClusterHealthAggregator` 整删。`ClusterHealthStatus` 枚举随 `Cluster.healthStatus` 字段移除而退场。

### Requirement: ClusterAffinityRouter 故障域亲和路由
**Reason**: `ClusterAffinityRouter` 的域级预判与端点级 HealthRouter 等价，且 `isForce=false` 的「DOWN 域过滤后让链继续」语义在删除域级聚合后无意义。RouterChain 不再含此路由器。
**Migration**: `ClusterAffinityRouter` 整删。RouterChain 顺序变为 `Permission → EndpointHealth → Priority → LoadBalance`。

## MODIFIED Requirements

### Requirement: Cluster 故障域实体

系统 SHALL 提供 `Cluster` 领域实体作为 Channel 的故障域分组（实体名保留，语义改造）。`Cluster` 表达**跨供应商的故障独立性分组**：同组 Channel 共享共因特征，但分组可跨供应商（如 OpenAI 官方 + Azure-OpenAI 同域），也可供应商内细分（同供应商多账号异域）。

**实体字段**（瘦身，删除原 region/priority/healthStatus）:
- `code` — 故障域编码，全局唯一（如 `openai-primary` / `azure-openai-shared` / `overseas-line`）
- `name` — 故障域名称
- `description` — 共因特征说明（如 "Azure-OpenAI 底层依赖 OpenAI 模型，共因"）
- 审计字段继承自 `BaseEntity`
- **删除** `region`（就近路由未实现）、`priority`（转移顺序归 ApplicationChannel.priority）、`healthStatus`（不持久化，域级聚合已删）

**关联**: `Channel.clusterId`（字段名保留不变）指向 `Cluster.id`。

**Cluster 与 providerId 共存正交**:
- `providerId` — 供应商标识（客观，管理面/计费/统计用），不作共因依据
- `clusterId` — 故障域归属（运维判断，L1 共因跳过依据）
- 两者正交：一个供应商的渠道可分属多域，一个域可含多供应商渠道

**API**（路径保留）:
- `POST /api/v1/resilience/clusters` — 创建故障域（HTTP 201）
- `PUT /api/v1/resilience/clusters/{id}` — 更新故障域
- `GET /api/v1/resilience/clusters/{id}` — 查询故障域详情
- `GET /api/v1/resilience/clusters` — 查询全部故障域列表

**规则**:
- 不提供 delete：Cluster 关联 Channel 的 clusterId，删除需级联清理

#### Scenario: 创建故障域

- **WHEN** 管理员调用 `POST /api/v1/resilience/clusters` 传入合法字段（code/name/description）
- **THEN** 系统 SHALL 创建 `Cluster` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201

#### Scenario: Channel 关联故障域

- **WHEN** Channel 配置 `clusterId` 指向某 Cluster
- **THEN** 该 Channel SHALL 归属该故障域
- **THEN** L1 共因跳过 SHALL 依据该 clusterId

#### Scenario: 跨供应商共因归同域

- **WHEN** OpenAI 官方渠道与 Azure-OpenAI 渠道配置相同 `clusterId`（因 Azure-OpenAI 底层依赖 OpenAI 模型，共因）
- **THEN** 两渠道 SHALL 归属同一故障域
- **THEN** L1 共因跳过时其一失败 SHALL 跳过另一个（即使 providerId 不同）

#### Scenario: 供应商内故障独立归异域

- **WHEN** 同供应商的两个账号渠道配置不同 `clusterId`（账号故障独立）
- **THEN** 两渠道 SHALL 归属不同故障域
- **THEN** L1 共因跳过时其一失败 SHALL NOT 跳过另一个（即使 providerId 相同）
