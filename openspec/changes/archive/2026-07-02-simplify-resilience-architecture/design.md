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

- [R1 PriorityRouter 排序器改动 BREAKING] → 集成测试覆盖主备 priority L1 转移（补当前单元测试绕过路由链的缺口）
- [R2 调谐下沉对象突变] → 每候选基于原始 request 派生副本调谐，测试覆盖候选不同 upstreamModelName
- [R3 共因跳过误杀] → 标记仅本次请求内有效不持久化 + 熔断器 HALF_OPEN 试探保底（折中方案）
- [R4 Cluster 语义迁移 BREAKING] → clusters 表删 region/priority/health_status 列；Channel.clusterId 字段名保留；管理面适配
- [R5 删 L2 能力回退] → 可接受，应用自决换模型；降级阶梯完整性让位于架构纯粹
- [R6 应用级 priority 迁移] → application_channels 加 priority 列，现有行 null 回退默认；ModelInstance.priority 删除前 grep 确认无其他用途
- [R7 转移事件 clusterId 语义不变] → failover_events 表 cluster_id 列保留，前端事件流展示适配共因跳过标记

## Migration Plan

1. DB 迁移（续接现有最大版本号）：clusters 表删 region/priority/health_status 列、application_channels 加 priority、删 model_instances.priority、删 resilience_profiles 表、failover_events 表 cluster_id 列保留
2. 代码改造按依赖顺序：PriorityRouter 排序器 → 调谐下沉 → 应用级 priority → 删 L2 → Cluster 语义改造+瘦身字段 → 删 DomainHealth → 删 PinnedModel/会话亲和 → ResilienceProfile 降级 → clusterId 共因跳过
3. 前端适配：容灾总览页、画像模板页（随 ResilienceProfile 退场）、Applications 页 priority 配置、Channels 页
4. spec 同步：整删 intelligent-degradation、cluster-failover 改造、其余 spec 修改
5. 回滚策略：DB 迁移不可逆（删列），回滚靠 git revert 代码 + 数据备份；建议生产前在仿真环境验证

## Open Questions

- `ApplicationChannel.priority` 的注入点：PermissionRouter 过滤时附着到候选，还是 RoutingRequest 携带应用→priority 映射？——build 阶段定
- `ModelInstance.priority` 删除前需 grep 确认无负载均衡/监控/前端展示用途
- 共因跳过激进程度：折中（共因跳过 + HALF_OPEN 试探保底），具体阈值 build 阶段定
- 转移次数上限 N 的取值，build 阶段定
- `convertRequest`（跨协议转换）是否需每候选独立，build 阶段评估
