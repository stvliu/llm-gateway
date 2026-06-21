# Model Instance Delta Spec

## MODIFIED Requirements

### Requirement: InstanceSelector.select 返回排序候选列表

`InstanceSelector.select` SHALL 由「返回单个实例」改为「返回按 (cluster, priority) 排序的候选列表」，供 L1 故障转移逐个尝试。

**变更要点**:
- 原 `select` 返回单个 `ModelInstance`（经 `LoadBalance` 收敛）
- 现 `select` 返回 `List<ModelInstance>`，按 `priority` 升序排序，不再收敛到单实例
- `LoadBalanceRouter`（`@Order(9999)`）降级为透传，不再执行负载均衡选择
- 候选列表经 `RouterChain` 过滤链产出：`Permission(@100) → Health(@200) → ClusterAffinity(@250) → Priority(@300) → PinnedModel(@350) → LoadBalance(@9999)`

**方法签名**:
```
List<ModelInstance> select(Long modelId, Long applicationId, Long userId, String role,
                           RoutingStrategy strategy, Protocol protocol)
```

**规则**:
- 候选按 `priority` 升序排序（由 `PriorityRouter` 保证）
- 无可用实例时抛 `ResourceNotFoundException`
- 解析容灾画像贯穿 `RoutingRequest`（fail-open：解析异常降级 null profile）

#### Scenario: 返回排序候选列表供 L1 逐个尝试

- **WHEN** `InstanceSelector.select(modelId, applicationId, userId, role, strategy, protocol)` 被调用
- **THEN** 系统 SHALL 返回按 `priority` 升序的候选 `ModelInstance` 列表
- **THEN** 列表 SHALL 供 `ChannelFailoverInvoker` 逐个尝试
- **THEN** 系统 SHALL NOT 收敛到单实例

#### Scenario: 无可用实例抛 ResourceNotFoundException

- **WHEN** 路由链过滤后候选列表为空
- **THEN** 系统 SHALL 抛出 `ResourceNotFoundException`

### Requirement: priority 语义重定义为 cluster 内排序

`ModelInstance.priority` 语义 SHALL 由「全局路由优先级」重定义为「cluster 内排序」。cluster 间顺序由 `Cluster.priority` 决定。

**变更要点**:
- 原 `priority` 为全局路由优先级，`PriorityRouter` 按全局 priority 升序分组
- 现 `priority` 为 cluster 内排序，cluster 间由 `Cluster.priority` 决定跨域转移顺序
- `PriorityRouter`（`@Order(300)`）仍按 `priority` 升序排序候选，但语义收敛到域内

**规则**:
- 同一 cluster 内：按 `ModelInstance.priority` 升序选择
- 跨 cluster：按 `Cluster.priority` 决定域优先级（数值越小越优先）
- 故障域内优先 → 整域故障才跨域

#### Scenario: 同 cluster 内按 priority 排序

- **WHEN** 同一 cluster 内有多个 `ModelInstance`
- **THEN** 系统 SHALL 按 `ModelInstance.priority` 升序排序
- **THEN** 优先选择 priority 最小的实例

#### Scenario: 跨 cluster 按 Cluster.priority 排序

- **WHEN** 候选跨多个 cluster
- **THEN** 系统 SHALL 按 `Cluster.priority` 决定域间优先级
- **THEN** 故障域内优先，整域故障才跨域
