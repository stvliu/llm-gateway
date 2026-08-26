# Comet Design Handoff

- Change: simplify-resilience-architecture
- Phase: design
- Mode: compact
- Context hash: 5eb434a30a3eeef8369dfe03ab73e57a10f9b81eb4d88326638d0d2afdf859ce

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/simplify-resilience-architecture/proposal.md

- Source: openspec/changes/simplify-resilience-architecture/proposal.md
- Lines: 1-44
- SHA256: d3b26f4de8bab3f4968230bc34a9cb0692bdf1da11ba3dcc9f054a5455bf0635

```md
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
- **DB schema**：clusters 表删 region/priority/health_status 列、application_channels 加 priority 列、删 model_instances.priority、删 resilience_profiles 表、failover_events 表 cluster_id 列保留
- **前端**：gateway-console 容灾总览页、画像模板页（随 ResilienceProfile 退场）、Applications 页（priority 配置）、Channels 页
- **API**：`/api/v1/resilience/profiles` 随 ResilienceProfile 退场调整；`/api/v1/resilience/clusters` 保留（Cluster 实体名不变，字段瘦身）；转移事件查询响应字段调整
- **依赖**：复用 `ResilientUpstreamClient`/`CircuitBreaker`/`KeyFailoverInvoker`，不重写上游客户端与重试/熔断算法
- **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为保持
```

## openspec/changes/simplify-resilience-architecture/design.md

- Source: openspec/changes/simplify-resilience-architecture/design.md
- Lines: 1-103
- SHA256: ca450e9a9b825a7b953a0915754132c43a4278aa669bcbef821bb67ab7ce7159

[TRUNCATED]

```md
## Context

`resilience-architecture` change 引入了四层容灾栈（L0/L1/L2/L3）+ Cluster 故障域 + 域级健康聚合。经架构探讨与复杂度审计，认定其存在两类问题：

1. **过度设计**：L2 模型降级有损静默且覆盖边缘场景；Cluster 域级健康聚合与端点级熔断等价、共因识别未被利用；PinnedModel 语义冗余；会话亲和收益依赖上游缓存；独立 ResilienceProfile 只剩 timeout。
2. **已存缺陷**：PriorityRouter 选择器语义（只留最优组 isForce=true）导致 L1 换不到备渠道；调谐只对首项做一次导致换渠道后请求 model 错误。

**设计原则**：网关不做「看起来智能但无实效」的中间判断，只提供通道（L0/L1）和让管理员/应用表达策略的旋钮；容灾止于通道，降级决策还给应用；架构匹配实际故障形态（LLM 供应商故障主要呈共因）。

**详细探讨记录**见 `C:\Users\liuye\.claude\plans\sprightly-hopping-rainbow.md`，本文件为其设计决策的 OpenSpec 镜像。

## Goals / Non-Goals

**Goals:**
- 容灾栈收敛为三层（L0 换 Key / L1 换渠道 + 共因跳过 / L3 抛错）
- Cluster 语义改造为跨供应商故障独立性分组（保留 `Cluster` 实体名，瘦身字段），驱动 L1 共因跳过
- 转移顺序改应用级 `ApplicationChannel.priority`，无主备只有先后次序
- 修复 PriorityRouter 选择器→排序器、调谐下沉每候选独立 两个已存缺陷
- 裁剪 L2/DomainHealth/PinnedModel/会话亲和/独立 ResilienceProfile

**Non-Goals:**
- 不重写上游客户端与重试/熔断算法（复用 ResilientUpstreamClient/CircuitBreaker/KeyFailoverInvoker）
- 不引入会话亲和（延后至确认有上游缓存收益）
- 不实现就近路由（region 上下文未实现）
- 不做 L2 模型降级（降级决策还给应用）
- 不改双 API 兼容（`/v1/chat/completions` 与 `/v1/messages` 行为保持）

## Decisions

### D1: 容灾止于通道，删除 L2 模型降级
L2 是收益最可疑的一层：覆盖的模型能力问题（上下文超长/能力缺失/模型缺陷）大多能被请求阶段能力匹配前置消解；降级链人工预配置、有损、静默，与「下游是应用而非人」「Token 成本透明」原则冲突；当前已默认关闭仅作可选兜底。删除后容灾栈收敛为 L0/L1/L3，降级决策还给应用（应用自己决定换不换模型）。
- **替代方案**：保留 L2 为可选默认关闭——降级阶梯完整但有损操作复杂度，企业内部网关 LLM 故障多为共因非模型能力问题，不划算，否决。
- **UNKNOWN_ERROR 归类**：删 L2 后 `ErrorClassifier` 中 `UNKNOWN_ERROR` → NONE（未分类错误不转移直接抛），`FailoverDecision` 收敛为 L1/NONE。

### D2: Cluster 语义改造为跨供应商故障独立性分组（保留实体名）
之前 Cluster 是「供应商内账号/区域细分」，已被否决（同供应商内细粒度共因是边缘情况，字段膨胀 region/priority/healthStatus）。Cluster 语义改为「跨供应商故障独立性分组」，表达 `providerId` 表达不了的关系：
- 跨供应商共因：OpenAI 官方 + Azure-OpenAI 底层依赖 OpenAI 模型 → 同域，providerId 不同
- 供应商内故障独立：同供应商多账号 → 异域，providerId 相同
- 专线共因：不同供应商走同一专线 → 同域
- **Cluster 与 providerId 共存正交**：providerId 是供应商标识（客观，管理面/计费用），clusterId 是故障域归属（运维判断，共因跳过依据）。
- **瘦身字段**：code/name/description + 审计（删 region/priority/healthStatus）。不加 region（就近未实现）/priority（排序归 ApplicationChannel）/providerId（不归属单一供应商），避免字段膨胀覆辙。
- **保留 `Cluster` 实体名与 `Channel.clusterId` 字段名**：不重命名，只改语义与瘦身字段，减少改动面。
- **替代方案**：删 Cluster 用 providerId 做共因跳过——providerId 表达不了跨供应商共因与供应商内故障独立，而这两者在企业内部网关确实出现（常接 Azure-OpenAI、常有同供应商多账号），否决。

### D3: 转移顺序改应用级 ApplicationChannel.priority
权限锚点是 `UserApiKey → Application → ApplicationChannel → Channel`，「换渠道顺序」结构上属于 ApplicationChannel（应用-渠道授权关系）。同一渠道对不同应用地位不同（渠道A 对客服是主用、对内部工具是备用），全局 `ModelInstance.priority` 无法表达。应用级 priority 完全取代全局，`ModelInstance.priority` 退场（避免两套 priority 埋坑）。
- **L1 语义**：无主备之分，只有先后次序——候选列表是有序队列，所有候选资格平等，区别仅在尝试顺序。

### D4: PriorityRouter 选择器→排序器（已存缺陷修复）
当前 `PriorityRouter` 只留 priority 最小组（isForce=true），`RouterChain` 对非空返回执行 `candidates=filtered`，候选被收敛到只剩最优组，备候选被永久丢弃，L1 无备可换。`ChannelFailoverInvokerTest` 直接构造 `List.of(ctx1,ctx2)` 绕过路由链，故单元测试绿灯但端到端 L1 在主备配置下失效。改为排序器：按应用级 priority 升序输出完整候选队列，不收敛。`ClusterAffinityRouter` 的「Health 先过滤让备上位」机制原本部分掩盖此缺陷，删 DomainHealth 后必须靠排序器兜底。

### D5: 调谐下沉 invoker，每候选独立（已存缺陷修复）
当前 `OutboundTuner.tune` 在 `ChatDispatchServiceImpl` 只对首项 `primaryCtx` 做一次，通道级调谐（`request.setModel(context.upstreamModelName())`）强依赖具体 RoutingContext。换到 candidate2/3 时复用同一调谐后请求，不同渠道 upstreamModelName 不同导致请求 model 错误，上游返回 INVALID_REQUEST/UNKNOWN → 删 L2 后判 NONE → 整个请求立即抛，后续候选永远没机会。下沉到 `ChannelFailoverInvoker` 内每候选试之前用自己的 RoutingContext 调谐。**对象突变陷阱**：当前 tune 原地修改 request，每候选调谐须基于原始 request 派生副本，不互相污染。`convertRequest`（跨协议转换）同源问题需一并评估。

### D6: L1 共因跳过（clusterId 驱动）
L1 转移由两个正交机制 + 一个保底机制协作：
- **先后次序**（策略维度）：候选队列按 `ApplicationChannel.priority` 排序，调用方定义
- **共因跳过**（拓扑维度）：当前候选共因失败（认证/限流/配额/网络/上游错误）→ 标记其 clusterId 为「本次共因失效」→ 跳过同域后续候选 → 试异域候选。标记仅本次请求内有效，不持久化
- **健康保底**（熔断器）：共因跳过处理「单点共因但域未全熔断」中间态，熔断器处理「已熔断」确定态

这是 Cluster 存在的核心理由——端点级熔断只知道「这个端点坏了」，不知道「同域的也大概率坏」，共因跳过是预测性转移，避免在注定一起挂的候选上空转，降低尾延迟。

### D6.5: 保留 Cluster 实体名
经探讨，`FailureDomain` 命名最终回退为 `Cluster`——保留现有 `Cluster` 实体名与 `Channel.clusterId` 字段名，只改语义（供应商内分组 → 跨供应商故障独立性分组）与瘦身字段（删 region/priority/healthStatus）。这减少了重命名改动面，且 `Cluster` 一词在容灾语境下可接受。语义差异由 spec 与字段瘦身体现，不靠改名。

### D7: 删除 DomainHealth 路由器
`ClusterHealthAggregator` + `ClusterAffinityRouter` 的域级预判在整域端点全 OPEN 时与端点级 HealthRouter 输出等价，价值有限。整域故障靠端点级熔断自然收敛 + 转移阶段 clusterId 共因跳过处理。Cluster 仅用于转移阶段共因跳过，不配域级聚合路由器。RouterChain 顺序变为 `Permission → EndpointHealth → Priority → LoadBalance`。

### D8: 删除 PinnedModel 与会话亲和
- **PinnedModel**：`enablePinnedModel`/`pinnedModelId` 语义冗余（请求已指定 modelId，路由本就按 modelId 找渠道），「应用只能用某模型」是授权职责（ApplicationChannel），不是容灾策略，删 `PinnedModelRouter` 与画像字段。
- **会话亲和**：LLM 调用多为无状态（每次带完整上下文），会话亲和收益依赖上游 prompt caching 等机制且不确定，`SessionAffinityStore`（Redis/InMemory 双实现）复杂度不低，延后至确认有缓存命中收益再做。

### D9: ResilienceProfile 实体降级
删 L2/PinnedModel/会话亲和后，ResilienceProfile 只剩 `timeout` 一个字段，不配独立实体。`timeout` 直接挂 `Application` 字段，ResilienceProfile 实体与 `resilience_profiles` 表退场，解析链 Application→Global 简化为 Application.timeout。

### D10: 转移事件流调整
`FailoverOccurredEvent`/`FailoverEvent` 的 clusterId 字段保留（语义随 Cluster 改造）；新增「是否共因跳过」标记（`commonCauseSkip`）。监听机制保持 `@EventListener` 同步持久化（调用链无 `@Transactional`，`@TransactionalEventListener` 会丢事件，已验证）。traceId 复用 `ChatDispatchServiceImpl` 已有 UUID 透传。

## Risks / Trade-offs

```

Full source: openspec/changes/simplify-resilience-architecture/design.md

## openspec/changes/simplify-resilience-architecture/tasks.md

- Source: openspec/changes/simplify-resilience-architecture/tasks.md
- Lines: 1-126
- SHA256: 720683d7cf341a19c577f2c8cdacf6792c91abe6b27f6780d5a8481c7b5df173

[TRUNCATED]

```md
# Tasks

> 实施顺序遵循 design.md 的依赖关系：先修两个已存缺陷（L1 正确性前提），再应用级 priority，再删 L2/DomainHealth/PinnedModel/会话亲和，再 ResilienceProfile 降级，最后 Cluster 改造与共因跳过。
> 每个任务 TDD：先写失败测试，再实现，再跑绿，再 commit。

## 1. 修复 PriorityRouter 选择器→排序器（已存缺陷，L1 前置）

- [ ] 1.1 grep 确认 `ModelInstance.priority` 的所有用途（负载均衡/监控/前端展示），记录是否可删
- [ ] 1.2 写失败测试：主备 priority 不同时，PriorityRouter 输出完整列表 [主,备] 不丢备（补当前单元测试绕过路由链的缺口）
- [ ] 1.3 写失败测试：RouterChain 经 PriorityRouter 后候选列表含全部 priority 组
- [ ] 1.4 改 `PriorityRouter.filter` 为排序器：按 priority 升序输出完整列表，不收敛；调整 `isForce` 语义
- [ ] 1.5 跑绿 + 回归 `./mvnw -pl gateway-boot -am test`
- [ ] 1.6 commit

## 2. 修复调谐下沉 invoker，每候选独立（已存缺陷，L1 前置）

- [ ] 2.1 写失败测试：候选不同 upstreamModelName 时，L1 换渠道后请求 model 正确（当前只对首项调谐导致换渠道后 model 错）
- [ ] 2.2 `ChannelFailoverInvoker` 注入 `OutboundTuner`，invoke/invokeStream 内每候选试之前基于原始 request 派生副本调谐
- [ ] 2.3 `ChatDispatchServiceImpl` 删阶段4对外调谐，改为传原始 request 给 invoker
- [ ] 2.4 评估 `convertRequest`（跨协议转换）是否需每候选独立，按需改造
- [ ] 2.5 跑绿 + 回归
- [ ] 2.6 commit

## 3. 应用级 ApplicationChannel.priority 取代全局（依赖 1）

- [ ] 3.1 `ApplicationChannel` 实体加 `priority` 字段 + Getter/Setter
- [ ] 3.2 Flyway 迁移：`application_channels` 加 `priority` 列
- [ ] 3.3 `ApplicationChannelGateway`/Impl/DO/Repository 适配 priority
- [ ] 3.4 `PermissionRouter` 过滤时把 ApplicationChannel.priority 附着到候选（或 RoutingRequest 携带映射）——定注入点
- [ ] 3.5 `PriorityRouter` 排序键改 ApplicationChannel.priority（无则回退默认）
- [ ] 3.6 `InstanceSelector.findActiveByModelIdOrderByPriority` 适配应用级排序
- [ ] 3.7 写测试：同渠道对不同应用不同 priority，各自转移顺序独立
- [ ] 3.8 跑绿 + 回归
- [ ] 3.9 commit

## 4. 删除 L2 模型降级层（独立）

- [ ] 4.1 整删 `application/degradation/` 包（DegradationService/Impl/Properties/Event/RecoveredEvent + `@Scheduled recoveryCheck`）
- [ ] 4.2 整删 `L2DegradationRequiredException`
- [ ] 4.3 `ChannelFailoverInvoker` 删 `tryL2Degradation`/`degradationService` 字段/构造参数，候选耗尽直接抛 lastException，签名去 profile
- [ ] 4.4 `ChatDispatchServiceImpl` 删 `invokeWithL2Failover`/`invokeStreamWithL2Failover`/`resolveMaxDepth`/`unwrapL2Cause`/`MAX_DEGRADATION_DEPTH`/`resolveProfileSafely`/`resilienceResolver` 依赖，直接调 invoker
- [ ] 4.5 `FailoverDecision` 删 L2 枚举值
- [ ] 4.6 `ErrorClassifier` UNKNOWN→NONE，getOrDefault 兜底改 NONE
- [ ] 4.7 删 `DegradationServiceTest`，适配 `ChannelFailoverInvokerTest`/`ChatDispatchServiceTest`/`ChannelFailoverIntegrationTest`
- [ ] 4.8 跑绿 + 回归
- [ ] 4.9 commit

## 5. 删除 DomainHealth 路由器（独立，与 6 无强依赖）

- [ ] 5.1 整删 `ClusterHealthAggregator`
- [ ] 5.2 整删 `ClusterAffinityRouter`，RouterChain 顺序变为 Permission→EndpointHealth→Priority→LoadBalance
- [ ] 5.3 删 `ClusterHealthStatus` 枚举（若 Cluster 不再用）
- [ ] 5.4 适配 `RouterChainTest`/`HealthRouterTest`
- [ ] 5.5 跑绿 + 回归
- [ ] 5.6 commit

## 6. Cluster 语义改造 + 瘦身字段（独立，保留实体名）

- [ ] 6.1 `Cluster` 实体删 region/priority/healthStatus 字段，保留 code/name/description + 审计；更新 Javadoc 语义为「跨供应商故障独立性分组」
- [ ] 6.2 `Channel.clusterId` 字段名保留不变
- [ ] 6.3 `ClusterGateway`/Impl/DO/Repository 删 region/priority/healthStatus 适配
- [ ] 6.4 Flyway 迁移：clusters 表删 region/priority/health_status 列
- [ ] 6.5 `ClusterController` 适配（字段瘦身，API 路径 `/api/v1/resilience/clusters` 保留）
- [ ] 6.6 `ChannelFailoverInvoker.publishFailoverEvent` 的 clusterId 反查逻辑保留（语义不变）
- [ ] 6.7 `FailoverOccurredEvent`/`FailoverEvent`/DO 的 clusterId 字段保留，新增 `commonCauseSkip` 标记
- [ ] 6.8 适配所有 Cluster 引用测试（删 region/priority/healthStatus 相关断言）
- [ ] 6.9 跑绿 + 回归
- [ ] 6.10 commit

## 7. 删除 PinnedModel 与会话亲和（独立）

- [ ] 7.1 整删 `PinnedModelRouter`
- [ ] 7.2 删 `ResilienceProfile.enablePinnedModel`/`pinnedModelId`（若 8 未先删实体）
- [ ] 7.3 整删 `SessionAffinityStore`（Redis/InMemory 双实现）+ `SessionAffinityConfig`
- [ ] 7.4 删 `ResilienceProfile.enableSessionAffinity`/`sessionAffinityTtlMinutes`
- [ ] 7.5 RouterChain 去除 PinnedModel（已在 5.2 处理，此处确认）
- [ ] 7.6 适配测试
- [ ] 7.7 跑绿 + 回归
- [ ] 7.8 commit

```

Full source: openspec/changes/simplify-resilience-architecture/tasks.md

## openspec/changes/simplify-resilience-architecture/specs/application-access-control/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/application-access-control/spec.md
- Lines: 1-49
- SHA256: 5e000fffcd1e9f9f5a228a32c347a70881bb7db38afc011d55ace8fbed039664

```md
# Application Access Control Delta Spec

## MODIFIED Requirements

### Requirement: ApplicationChannel 渠道可见性

系统 SHALL 通过 `ApplicationChannel` 关联实体决定应用可见的渠道集合，并承载应用级转移顺序（priority）。

**实体字段**: `applicationId`（应用 ID）、`channelId`（渠道 ID）、`priority`（应用级转移顺序，数值越小越先试）；唯一约束 `(application_id, channel_id)`。

**新增字段 `priority`**:
- 应用级转移顺序，L1 候选列表按此升序排序
- 同一渠道对不同应用可有不同 priority（渠道A 对客服应用 priority=1，对内部工具 priority=3）
- 完全取代原全局 `ModelInstance.priority`（ModelInstance.priority 退场）
- 无主备之分，只有先后次序——所有候选资格平等，区别仅在尝试顺序
- 为 null 时回退默认值

**API**:
- `GET /api/v1/applications/{id}/channels` — 查询应用授权的渠道列表（含 priority）
- `PUT /api/v1/applications/{id}/channels` — 更新应用渠道授权（先清空旧关联，再批量保存新关联含 priority；HTTP 204）
  - Request Body: `{ "channels": [{ "channelId": 1, "priority": 1 }, { "channelId": 2, "priority": 2 }] }`

**规则**:
- 模型可见性不独立配置——由「渠道上挂哪些 ModelInstance」隐式决定
- 要某模型就授权挂该模型的渠道；无法「授权渠道但限模型」
- 转移顺序由管理员通过 ApplicationChannel.priority 在前端定义，跨供应商是 priority 排序的自然结果

#### Scenario: 查询应用渠道授权含 priority

- **WHEN** 管理员调用 `GET /api/v1/applications/{id}/channels`
- **THEN** 系统 SHALL 返回该应用授权的渠道列表，每项含 `channelId` 与 `priority`

#### Scenario: 更新应用渠道授权含 priority 全量替换

- **WHEN** 管理员调用 `PUT /api/v1/applications/{id}/channels` 传入含 priority 的 channels 集合
- **THEN** 系统 SHALL 先清空该应用的原有 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 批量保存新的 `ApplicationChannel` 关联（含 priority）
- **THEN** 系统 SHALL 返回 HTTP 204

#### Scenario: 同渠道对不同应用不同 priority

- **WHEN** 渠道A 对客服应用配置 priority=1，对内部工具应用配置 priority=3
- **THEN** L1 候选排序 SHALL 按各自应用的 priority 独立排序
- **THEN** 客服应用候选列表渠道A 排前，内部工具应用候选列表渠道A 排后

#### Scenario: 渠道授权为空时无可用渠道

- **WHEN** 应用的 `ApplicationChannel` 授权集合为空
- **THEN** 该应用的所有 API Key 无可用渠道（自然拒绝）
```

## openspec/changes/simplify-resilience-architecture/specs/application/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/application/spec.md
- Lines: 1-45
- SHA256: acdcb687c5cfc4c310930c4846be151823b422f2da76edea9e7e2d1f0636e2d8

```md
# Application Delta Spec

## MODIFIED Requirements

### Requirement: Application 根实体实体

系统 SHALL 提供 `Application` 根实体实体作为「权限 + 行为」双根实体，承载 N 把 Key 的应用归属、渠道可见性、应用级超时，并预留配额/看板字段。

**实体字段**（移除 `resilienceProfileId`，新增 `timeout`）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态（`ApplicationState`，控制是否可路由）
- `timeout` — 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout，ResilienceProfile 实体退场）
- `quotaBudgetId` — 配额预算 ID（预留，留 quota 域填充）
- `dashboardId` — 看板 ID（预留，留 audit 域填充）
- 审计字段 `createdBy/createdAt/updatedBy/updatedAt` 继承自 `BaseEntity`

**移除字段**:
- `resilienceProfileId` — ResilienceProfile 实体退场，不再关联画像

**API**:
- `POST /api/v1/applications` — 创建应用（请求体 `code/name/description/timeout`，返回 `ApplicationResponse`，HTTP 201）
- `PUT /api/v1/applications/{id}` — 更新应用
- `GET /api/v1/applications/{id}` — 查询应用详情
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（级联清理渠道授权关联，HTTP 204）

#### Scenario: 创建应用

- **WHEN** 管理员调用 `POST /api/v1/applications` 传入合法 `code/name/description/timeout`
- **THEN** 系统 SHALL 创建 `Application` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的 `ApplicationResponse`

#### Scenario: 删除应用级联清理渠道授权

- **WHEN** 管理员调用 `DELETE /api/v1/applications/{id}` 删除应用
- **THEN** 系统 SHALL 级联清理该应用的 `ApplicationChannel` 关联
- **THEN** 系统 SHALL 返回 HTTP 204

## REMOVED Requirements

### Requirement: Application 承载容灾画像绑定
**Reason**: ResilienceProfile 实体退场（删 L2/PinnedModel/会话亲和后只剩 timeout，不配独立实体）。`Application.resilienceProfileId` 关联移除，timeout 直接挂 Application 字段。
**Migration**: `PUT /api/v1/applications/{id}/resilience` 端点移除。timeout 通过应用 CRUD 端点（创建/更新）直接配置。容灾画像解析链（Application→Global）退场，timeout 由 `Application.timeout` 直接提供。
```

## openspec/changes/simplify-resilience-architecture/specs/channel-failover/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/channel-failover/spec.md
- Lines: 1-159
- SHA256: 43958512e6bb85e645b8ac08dd3aa9c260d9530df881af6dc6d128a22f56dfc7

[TRUNCATED]

```md
# Channel Failover Delta Spec

## MODIFIED Requirements

### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

系统 SHALL 提供 `ChannelFailoverInvoker` 作为 Channel 级（L1）运行时失败转移回路，在候选列表内逐个尝试，作为主转移路径。

**三层容灾栈**（删除 L2 模型级）:
- L0 Key 级（同渠道换 Key，复用 `KeyFailoverInvoker`）
- L1 Channel 级（同模型换渠道 + clusterId 共因跳过，本 capability 核心）
- L3 抛错

**调用入口**（移除 profile 参数，L2 门禁随 L2 删除）:
- `invoke(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId)` — 非流式
- `invokeStream(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId, StreamCallback callback)` — 流式

**每候选独立调谐**（修复原只对首项调谐一次的缺陷）：试每个候选前 SHALL 基于原始请求派生副本，用该候选的 `RoutingContext` 独立调谐（通道级模型名替换按该候选 `upstreamModelName`），不携带前一候选痕迹。

#### Scenario: L1 候选内逐个尝试

- **WHEN** `ChannelFailoverInvoker.invoke` 接收按 ApplicationChannel.priority 排序的候选列表
- **THEN** 系统 SHALL 对每个候选依次调用 `KeyFailoverInvoker.invoke`（内部跑 L0 Key 级转移）
- **THEN** 某候选成功时 SHALL 立即返回响应

#### Scenario: L1 候选失败按错误分流决策

- **WHEN** 某候选调用抛出 `ProviderException`
- **THEN** 系统 SHALL 经 `ErrorClassifier.classify(errorType)` 得到 `FailoverDecision`
- **THEN** 决策为 `NONE` 时 SHALL 直接抛出原异常（不转移）
- **THEN** 决策为 `L1` 时 SHALL 发布转移事件后继续尝试下一候选

#### Scenario: L1 全耗尽抛最后异常（删除 L2）

- **WHEN** 所有 L1 候选均失败
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

```

Full source: openspec/changes/simplify-resilience-architecture/specs/channel-failover/spec.md

## openspec/changes/simplify-resilience-architecture/specs/channel-health-tracking/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/channel-health-tracking/spec.md
- Lines: 1-36
- SHA256: 9f9c9285173bc31e271caedf4d80a78c697bb81acd453ec510f783a9aeb847bb

```md
# Channel Health Tracking Delta Spec

## MODIFIED Requirements

### Requirement: 熔断 key 统一为 endpointId

熔断 key SHALL 统一为 `endpointId`，路由侧（`HealthRouter`）与调用侧（`KeyFailoverInvoker`）共享同一熔断器实例（`ChannelEndpointCircuitBreakerManager`）。

**变更要点**:
- 原 `HealthRouter` 用 `channelId` 查熔断，`KeyFailoverInvoker` 用 `endpointId`，路由侧与调用侧熔断互不可见
- 现 `HealthRouter`（`@Order(200)`）用 `channelId + protocol` 经 `EndpointResolver` 派生 `endpointId` 后查 `ChannelEndpointCircuitBreakerManager`
- 与 `KeyFailoverInvoker`（用 `RoutingContext.channelEndpointId()`）共享同一 manager bean
- 不向 `ModelInstance`/`model_instances` 表加 `endpointId` 字段（channel 粒度与 channel×protocol 端点粒度 1:1 不自洽，采用运行时派生方案）

**RouterChain 顺序修正**（删除 ClusterAffinity 与 PinnedModel）:
- 原 `Permission(@100) → Health(@200) → ClusterAffinity(@250) → Priority(@300) → PinnedModel(@350) → LoadBalance(@9999)`
- 现 `Permission(@100) → EndpointHealth(@200) → Priority(@300) → LoadBalance(@9999)`
- Health 先于 Priority（次优先级健康渠道能成为转移候选）
- Priority 为排序器（输出完整候选列表，不收敛），非选择器

#### Scenario: 路由侧与调用侧共享熔断器

- **WHEN** `HealthRouter` 过滤熔断渠道
- **THEN** 系统 SHALL 用 `channelId + protocol` 派生 `endpointId` 查 `ChannelEndpointCircuitBreakerManager`
- **THEN** 该 manager SHALL 与 `KeyFailoverInvoker` 共享同一 bean 实例

#### Scenario: Health 先于 Priority 使次优先级渠道可被选

- **WHEN** 主优先级渠道熔断，次优先级渠道健康
- **THEN** `HealthRouter` SHALL 先过滤掉熔断渠道
- **THEN** `PriorityRouter` SHALL 在存活渠道里按 priority 排序输出完整列表
- **THEN** 次优先级健康渠道 SHALL 保留在候选列表中成为转移候选

## REMOVED Requirements

> 注：原 `ProviderHealthTracker 收窄为供应商级粗粒度信号` Requirement 不变（仍收窄为 L2 备选参考，但 L2 已删，仅供参考），本 delta 不修改它。域级健康聚合与亲和路由的删除在 cluster-failover delta 中处理（`ClusterHealthAggregator`/`ClusterAffinityRouter` 属 cluster-failover capability）。
```

## openspec/changes/simplify-resilience-architecture/specs/cluster-failover/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/cluster-failover/spec.md
- Lines: 1-66
- SHA256: f281ab6528d57896c301e5f7ad89166d638c4773bd34e551e4ef428424b536db

```md
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

系统 SHALL 提供 `Cluster` 实体作为 Channel 的故障域分组（实体名保留，语义改造）。`Cluster` 表达**跨供应商的故障独立性分组**：同组 Channel 共享共因特征，但分组可跨供应商（如 OpenAI 官方 + Azure-OpenAI 同域），也可供应商内细分（同供应商多账号异域）。

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
```

## openspec/changes/simplify-resilience-architecture/specs/intelligent-degradation/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/intelligent-degradation/spec.md
- Lines: 1-27
- SHA256: c2a5491f19e4ecf61a3c4f4404ad2f9f63b6a9144a4adb06d155e1b054de3a39

```md
# Intelligent Degradation Delta Spec

## REMOVED Requirements

### Requirement: 降级链配置
**Reason**: L2 模型降级层整体移除。降级是「有损的最后手段」，覆盖的模型能力问题（上下文超长/能力缺失/模型缺陷）大多能被请求阶段能力匹配前置消解；降级链人工预配置、有损、静默，与「下游是应用而非人」「Token 成本透明」原则冲突。容灾止于通道（L0/L1），降级决策还给应用。
**Migration**: 应用如需换模型能力，由应用自身决定（不依赖网关运行时降级）。`DegradationService`/`DegradationServiceImpl`/`DegradationProperties`/`DegradationEvent`/`DegradationRecoveredEvent` 整删，`@Scheduled recoveryCheck` 一并删除。

### Requirement: 降级触发
**Reason**: L2 模型降级层移除，降级触发条件不再适用。共因故障由 L1 Channel 级转移处理，请求级错误直接抛出，模型能力问题交给应用。
**Migration**: 见「降级链配置」迁移说明。

### Requirement: 降级通知
**Reason**: L2 降级移除，降级事件（`DegradationEvent`/`DegradationRecoveredEvent`）不再产生。
**Migration**: 事件类整删。容灾可观测性由转移事件流（`FailoverOccurredEvent`）承载。

### Requirement: 自动回切
**Reason**: L2 降级移除，`recoveryCheck` 定时回切不再需要。
**Migration**: `@Scheduled recoveryCheck` 与相关逻辑整删。

### Requirement: Metrics 埋点
**Reason**: L2 降级移除，降级相关 Metrics（`gateway.degradation.*`）不再产生。
**Migration**: 降级 Metrics 整删；转移 Metrics（`gateway.failover.*`）保留。

### Requirement: L2 降级信号由上层重路由
**Reason**: L2 降级移除，`L2DegradationRequiredException` 信号机制不再存在。L1 候选全耗尽后直接抛最后异常，不再进 L2 重路由。
**Migration**: `L2DegradationRequiredException` 整删；`ChatDispatchServiceImpl` 的 `invokeWithL2Failover`/`invokeStreamWithL2Failover`/`resolveMaxDepth`/`unwrapL2Cause`/`MAX_DEGRADATION_DEPTH` 整删，改为直接调用 `ChannelFailoverInvoker`。
```

## openspec/changes/simplify-resilience-architecture/specs/model-instance/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/model-instance/spec.md
- Lines: 1-7
- SHA256: 1374acde517ff5440040f5dcce55511990a732f9a0d2c49defce356639b755c1

```md
# Model Instance Delta Spec

## REMOVED Requirements

### Requirement: 路由优先级在 ModelInstance 级别
**Reason**: 转移顺序改由应用级 `ApplicationChannel.priority` 承载。同一渠道对不同应用地位不同（渠道A 对客服是主用、对内部工具是备用），全局 `ModelInstance.priority` 无法表达这种应用级差异。应用级 priority 完全取代全局，ModelInstance.priority 退场，避免两套 priority 埋坑。
**Migration**: `ModelInstance.priority` 字段删除，`model_instances.priority` 列删除。`PriorityRouter` 排序键改为 `ApplicationChannel.priority`。`InstanceSelector` 的 `findActiveByModelIdOrderByPriority` 改为按应用级 priority 排序（经 ApplicationChannel 注入）。
```

## openspec/changes/simplify-resilience-architecture/specs/resilience-console/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/resilience-console/spec.md
- Lines: 1-56
- SHA256: a1a8e8639d8920bf70cc7f823e34f89530eac3ddb77d9c3dd3461511a2e928bb

```md
# Resilience Console Delta Spec

## MODIFIED Requirements

### Requirement: 容灾总览页

容灾总览页 SHALL 展示故障域拓扑、转移事件流与耗尽告警，移除降级/会话亲和/PinnedModel 相关展示。

**变更要点**:
- 故障域拓扑：展示 Cluster 分组（语义为跨供应商故障独立性分组），渠道按 clusterId 归域
- 转移事件流：事件字段 `clusterId` 保留，新增「是否共因跳过」标记展示
- 耗尽告警：保留（候选全耗尽事件）
- 移除：降级事件展示（L2 删除）、会话亲和状态、PinnedModel 状态

#### Scenario: 展示故障域拓扑

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL 展示 Cluster 分组拓扑，渠道按 clusterId 归域
- **THEN** 页面 SHALL NOT 展示已删除的 Cluster region/priority 字段

#### Scenario: 转移事件流展示 clusterId

- **WHEN** 转移事件流渲染事件
- **THEN** 事件 SHALL 展示 from→to 渠道、clusterId、错误类型、决策、是否共因跳过
- **THEN** 页面 SHALL 高亮共因跳过与耗尽事件

### Requirement: 应用管理页容灾模式选择

应用管理页 SHALL 移除容灾画像绑定，改为应用级 timeout 配置与渠道 priority 排序。

**变更要点**:
- 移除：容灾画像模板选择（ResilienceProfile 退场）、容灾模式档位选择
- 新增：应用 timeout 配置（直接在应用编辑表单）
- 新增：渠道授权页支持 priority 配置（拖拽或数值排序，定义 L1 转移先后次序）

#### Scenario: 应用编辑配置 timeout

- **WHEN** 管理员在应用编辑页配置 timeout
- **THEN** 系统 SHALL 保存到 `Application.timeout`
- **THEN** 页面 SHALL NOT 展示容灾画像绑定入口

#### Scenario: 渠道授权配置 priority

- **WHEN** 管理员在应用渠道授权页配置各渠道 priority
- **THEN** 系统 SHALL 保存到 `ApplicationChannel.priority`
- **THEN** 页面 SHALL 展示渠道按 priority 排序的先后次序

## REMOVED Requirements

### Requirement: 画像模板页
**Reason**: ResilienceProfile 实体退场（删 L2/PinnedModel/会话亲和后只剩 timeout，不配独立实体），画像模板页失去载体。
**Migration**: 画像模板页（CRUD + 专家字段折叠）整删。`ResilienceProfileController` 与相关前端页面/路由移除。timeout 改在应用编辑页配置。

### Requirement: Channels 一键应急操作
**Reason**: 本 change 范围聚焦容灾架构简化，Channels 应急操作（一键熔断/恢复/紧切域）依赖的「域」概念语义已改造（供应商内分组 → 跨供应商故障独立性分组），且「紧切域」依赖域级亲和路由（已删 ClusterAffinityRouter）。应急操作需基于新 Cluster 语义重新设计，超出本次简化范围。
**Migration**: Channels 应急操作的「紧切域」功能移除（依赖已删的域级路由）；「一键熔断/恢复」保留（基于端点级熔断器 forceOpen/forceClose，不依赖域）。后续如需基于 Cluster 的应急操作，另开 change。
```

## openspec/changes/simplify-resilience-architecture/specs/resilience-profile/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/resilience-profile/spec.md
- Lines: 1-29
- SHA256: 1bbdfc019dbc42bef74a9804dc1ee150b54cd9cd284b16a0699ca85e7f244eb7

```md
# Resilience Profile Delta Spec

> ResilienceProfile 实体降级：删除 L2/PinnedModel/会话亲和后只剩 timeout，直接挂 Application，不独立成实体。

## REMOVED Requirements

### Requirement: ResilienceProfile 容灾画像实体
**Reason**: 删除 L2/PinnedModel/会话亲和后，ResilienceProfile 只剩 `timeout` 一个字段，不配独立实体。`timeout` 直接挂 `Application` 字段，ResilienceProfile 实体与 `resilience_profiles` 表退场。
**Migration**: `ResilienceProfile` 实体、`ResilienceProfileGateway`/Impl、`resilience_profiles` 表、`ResilienceProfileController`、`ResilienceProfileApplier` 整删。`Application.resilienceProfileId` 关联移除，新增 `Application.timeout` 字段。

### Requirement: 解析链 Application → Global
**Reason**: ResilienceProfile 实体退场，解析链失去载体。timeout 直接从 Application 读取。
**Migration**: `ResilienceResolver` 整删。timeout 由 `Application.timeout` 直接提供，无解析链。

### Requirement: 容灾模式档位推导
**Reason**: ResilienceProfile 退场，档位（default/strict/aggressive/batch）推导失去载体。L2 已删（strict 档关 L2 的语义无意义），PinnedModel/会话亲和删，档位无可推导字段。
**Migration**: `ResilienceProfileApplier` 整删，seed 数据（`V56__seed_resilience_profiles.sql`）移除。

### Requirement: 会话亲和
**Reason**: LLM 调用多为无状态（每次带完整上下文），会话亲和收益依赖上游 prompt caching 等机制且不确定，复杂度不低。延后至确认有缓存命中收益再做。
**Migration**: `SessionAffinityStore`（Redis/InMemory 双实现）、`SessionAffinityConfig`、画像 `enableSessionAffinity`/`sessionAffinityTtlMinutes` 字段整删。

### Requirement: 画像门禁 L2 降级
**Reason**: L2 模型降级删除，画像 L2 门禁失去对象。
**Migration**: `enableL2ModelDegradation`/`degradationMaxDepth` 字段删除。

### Requirement: 画像解析 fail-open
**Reason**: ResilienceProfile 退场，画像解析 fail-open 机制失去载体。timeout 直接从 Application 读取（Application 必然存在，无需 fail-open）。
**Migration**: `InstanceSelector.resolveProfileSafely`/`ChatDispatchServiceImpl.resolveProfileSafely` 整删。
```

## openspec/changes/simplify-resilience-architecture/specs/upstream-exception-classification/spec.md

- Source: openspec/changes/simplify-resilience-architecture/specs/upstream-exception-classification/spec.md
- Lines: 1-53
- SHA256: 001a13c3a8c521d9fabac08f2cf9edb308bc1a5d468f2c82996eac3b8e3fc64d

```md
# Upstream Exception Classification Delta Spec

## MODIFIED Requirements

### Requirement: 错误分类接入错误分流表驱动转移决策

上游异常分类结果（`ProviderErrorType`）SHALL 接入错误分流表（`ErrorClassifier`），驱动 L1/NONE 转移决策。

**变更要点**（删除 L2，UNKNOWN 改 NONE）:
- 原错误分类经 `ErrorClassifier.classify(errorType)` 映射到 `FailoverDecision`（L1/L2/NONE）
- 现 `FailoverDecision` 收敛为 L1/NONE（删除 L2）
- `UNKNOWN_ERROR` 由 L2 改为 NONE（未分类错误不转移直接抛，降级决策还给应用）

**分流规则**（错误分流表）:
- `INVALID_REQUEST` → `NONE`（请求级错误，换哪都无效，直接抛出）
- 共因故障（`AUTHENTICATION_ERROR`/`RATE_LIMIT_ERROR`/`QUOTA_EXCEEDED`/`TIMEOUT_ERROR`/`UPSTREAM_ERROR`/`SERVICE_UNAVAILABLE`/`NETWORK_ERROR`）→ `L1`（换渠道）
- `UNKNOWN_ERROR` → `NONE`（未分类错误，不转移直接抛）
- `null` 输入 → `NONE`（编程错误或未分类，直接抛出不转移）
- 未在表中显式映射的新增枚举值 → `NONE`（兜底不转移，直接抛）

**ProviderErrorType 分类来源**（HTTP 状态码映射不变）:

| HTTP 状态码 | ProviderErrorType | 分流决策 |
|------------|------------------|---------|
| 401 | AUTHENTICATION_ERROR | L1 |
| 429（不含 quota） | RATE_LIMIT_ERROR | L1 |
| 429（含 quota/insufficient_quota） | QUOTA_EXCEEDED | L1 |
| 400 | INVALID_REQUEST | NONE |
| 408 / ReadTimeout | TIMEOUT_ERROR | L1 |
| 500 / 502 | UPSTREAM_ERROR | L1 |
| 503 | SERVICE_UNAVAILABLE | L1 |
| 504 | TIMEOUT_ERROR | L1 |
| 529（Anthropic 过载） | UPSTREAM_ERROR | L1 |
| IOException | NETWORK_ERROR | L1 |
| 其他 | UNKNOWN_ERROR | NONE |

#### Scenario: 错误分类驱动 L1 换渠道

- **WHEN** 上游返回 HTTP 401，`ProviderErrorType = AUTHENTICATION_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `L1`
- **THEN** `ChannelFailoverInvoker` SHALL 换下一候选渠道

#### Scenario: 错误分类驱动 NONE 不转移

- **WHEN** 上游返回 HTTP 400，`ProviderErrorType = INVALID_REQUEST`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移

#### Scenario: 未分类错误不转移

- **WHEN** 上游返回未分类状态码，`ProviderErrorType = UNKNOWN_ERROR`
- **THEN** `ErrorClassifier.classify` SHALL 返回 `NONE`
- **THEN** `ChannelFailoverInvoker` SHALL 直接抛出原异常，不转移（降级决策还给应用）
```

