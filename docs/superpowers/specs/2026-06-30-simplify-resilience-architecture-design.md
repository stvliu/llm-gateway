---
comet_change: simplify-resilience-architecture
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-02-simplify-resilience-architecture
status: final
---

# Simplify Resilience Architecture — 容灾架构简化设计

> Comet change `simplify-resilience-architecture` 的 Superpowers Design Doc。详细 OpenSpec 产物见 `openspec/changes/simplify-resilience-architecture/design.md`，本文件为其镜像与实现细节补充。

## Context

`resilience-architecture` change 引入的四层容灾栈（L0/L1/L2/L3）+ Cluster 域级聚合存在过度设计与已存缺陷。经架构探讨与复杂度审计，认定应收敛为「解决实际问题的最小充分集」：容灾止于通道（L0/L1），降级决策还给应用；Cluster 语义改造为跨供应商故障独立性分组、驱动共因跳过。

**两类问题**：
1. **过度设计**：L2 有损静默覆盖边缘场景；Cluster 域级聚合与端点级熔断等价、共因未利用；PinnedModel 语义冗余；会话亲和收益依赖上游缓存；独立 ResilienceProfile 只剩 timeout。
2. **已存缺陷**：PriorityRouter 选择器语义（isForce=true 收敛到最优组）导致 L1 换不到备渠道；调谐只对首项做一次导致换渠道后请求 model 错误。单元测试绕过路由链未覆盖。

## Goals / Non-Goals

**Goals:**
- 容灾栈收敛为三层（L0 换 Key / L1 换渠道 + 共因跳过 / L3 抛错）
- Cluster 语义改造为跨供应商故障独立性分组（保留实体名，瘦身字段），驱动 L1 共因跳过
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

架构层决策 D1-D10 见 OpenSpec `design.md`（删 L2、Cluster 语义改造、应用级 priority、PriorityRouter 排序器、调谐下沉、共因跳过、删 DomainHealth、删 PinnedModel/会话亲和、ResilienceProfile 降级、转移事件流）。本文件补充**实现层面技术决策** ID1-ID5。

### ID1: ApplicationChannel.priority 注入 — RoutingRequest 携带映射

`RoutingRequest` 删 `resilienceProfile` 字段（随 ResilienceProfile 退场），改为携带 `Map<Long channelId, Integer priority>`。

- `InstanceSelector.select` 构造 RoutingRequest 前，查 `ApplicationChannelGateway.findByApplicationId(applicationId)` 取该应用所有授权渠道的 priority，构建 `Map<channelId, priority>` 填入 RoutingRequest
- `PermissionRouter` 不变（仍按 channelId 过滤可见渠道）
- `PriorityRouter` 从 `request.getChannelPriorityMap()` 取映射，按 `map.get(mi.getChannelId())` 升序排序（null 回退默认值 100）
- **不污染 ModelInstance 领域实体**：遵循「领域模型纯洁性」铁律，priority 是路由上下文数据，不挂实体

### ID2: 共因跳过 — RoutingContext 带 clusterId + 局部 Set 标记

`RoutingContext` record 增加 `clusterId` 字段，由 `RoutingResolver` 构造时从 `Channel.clusterId` 填充。

- 一次填充多处复用：共因跳过判断 + `publishFailoverEvent`（省掉现有 ChannelGateway 反查）
- `ChannelFailoverInvoker.invoke/invokeStream` 内局部 `Set<Long> commonCauseFailedClusters`：
  - 试候选前：若 `commonCauseFailedClusters.contains(candidate.clusterId())`，跳过该候选，发转移事件（`commonCauseSkip=true`），不计入 lastException
  - 候选共因失败（`FailoverDecision=L1`）时：`commonCauseFailedClusters.add(candidate.clusterId())`，发转移事件（`commonCauseSkip=false`），记录 lastException
  - 标记仅方法内局部，方法结束即丢弃（天然本次请求有效，不持久化）
- 所有剩余候选都被共因跳过 → 抛 lastException（不改变全耗尽语义，加速到达耗尽）
- **误杀保底**：共因跳过不持久化 + 熔断器 HALF_OPEN 试探独立运作（域级恢复由熔断器客观反映）

### ID3: 调谐下沉 — convertRequest + tune 一起下沉，copy() 派生副本

`convertRequest`（跨协议转换）与 `OutboundTuner.tune`（协议级+通道级调谐）都依赖具体候选 RoutingContext，一起下沉到 `ChannelFailoverInvoker`。

- `ChatDispatchServiceImpl` 删阶段3（convertRequest）与阶段4（tune），传**原始入站 request** 给 invoker
- `ChannelFailoverInvoker` 注入 `OutboundTuner` + `ProtocolConverter`，每候选试之前：
  1. `ProtocolRequest candidateReq = originalRequest.copy()` — 派生副本
  2. 若 `candidate.needsProtocolAdaptation()`：`candidateReq = convertRequest(candidateReq, candidate)`
  3. `candidateReq = outboundTuner.tune(candidateReq, candidate)`
  4. `keyFailoverInvoker.invoke(candidate, candidateReq)`
- **副本机制**：`ProtocolRequest` 接口加 `copy()` 方法，`OpenAIChatRequest`/`AnthropicMessagesRequest` 各自实现字段拷贝。选手写字段拷贝而非 Jackson 深拷贝，避免性能开销与循环引用风险
- **不累积污染**：每候选都从原始入站 request 派生，原始 request 不被修改，候选间相互独立
- 流式 `invokeStream` 同理

### ID4: DB 迁移 — V51-V58 在 master，新加 V59-V64

V51-V58 全部已合并 master，不可改，必须新加迁移：

| 版本 | 变更 | 依赖 |
|------|------|------|
| V59 | application_channels 加 priority 列（INT NULL） | - |
| V60 | clusters 删 region/priority/health_status 列 | - |
| V61 | applications 加 timeout 列 + 删 resilience_profile_id 列 | - |
| V62 | 删 resilience_profiles 表 + 删 V56 seed 数据 | V61（先删外键引用） |
| V63 | model_instances 删 priority 列 | V59（priority 改应用级后） |
| V64 | failover_events 加 common_cause_skip 列（BOOLEAN DEFAULT FALSE） | - |

- **删 resilience_profiles 表不可逆**：生产前确认无应用依赖（ResilienceProfile 上 change 才引入，预期无生产数据，风险可控）
- 迁移顺序：V61 必须先于 V62（外键约束）

### ID5: 测试策略 — 集成测试补路由链缺口

当前 `ChannelFailoverInvokerTest` 直接构造 `List.of(ctx1, ctx2)` 绕过路由链，未覆盖 PriorityRouter 收敛缺陷与调谐缺陷。补三类集成测试（经完整 RouterChain 产出候选）：

- **路由链主备转移 + 调谐正确**：主备 priority 不同 → 候选含 [主,备] 不丢备 → 主失败换备；候选不同 upstreamModelName → 换渠道后请求 model 正确
- **共因跳过跨域**：同 clusterId 共因失败 → 跳过同域 → 试异域；跨供应商共因（OpenAI+Azure-OpenAI 同 clusterId）→ 共因跳过；共因跳过发转移事件 commonCauseSkip=true
- **删除回归**：L1 全耗尽抛最后异常（不进 L2）；UNKNOWN_ERROR→NONE 不转移；timeout 从 Application 读

测试层次：
- 单元测试：ErrorClassifier/PriorityRouter 逻辑保留；ChannelFailoverInvokerTest 删 L2 场景，新增共因跳过场景，候选改为路由链产出
- 集成测试：Spring 上下文 + H2 覆盖端到端 L1 转移
- 仿真测试：provider-simulator 验证供应商级共因故障跨域转移
- 删除测试：DegradationServiceTest/ResilienceProfileIntegrationTest/SessionAffinityStoreTest 等直接删

## Risks / Trade-offs

- [R1 RoutingContext 加 clusterId] record 字段扩张，但避免 invoker 反查 DB（转移是失败路径）。一次填充多处复用。
- [R2 ProtocolRequest.copy() 手写] 接口扩张，各实现类实现 copy。代价小于 Jackson 深拷贝的性能开销与循环引用风险。
- [R3 convertRequest 一起下沉] 若不下沉，跨协议候选换渠道后协议转换错误（留缺陷）。下沉后每候选独立 convert+tune。
- [R4 删 resilience_profiles 表不可逆] V62 删表。生产前确认无应用依赖，风险可控。
- [R5 共因跳过误杀] 标记仅本次请求有效不持久化 + 熔断器 HALF_OPEN 试探保底。折中方案。
- [R6 priority 映射 null 回退] ApplicationChannel.priority 为 null 时回退默认值 100，避免 NPE。
- [R7 PriorityRouter 排序器 BREAKING] 集成测试覆盖主备 priority L1 转移（补当前单元测试绕过路由链的缺口）。
- [R8 调谐下沉对象副本] 每候选基于原始 request copy 派生，不互相污染。测试覆盖候选不同 upstreamModelName。

## Data Flow

```
请求 → ApiKeyAuthInterceptor(解析 applicationId)
  → InstanceSelector.select:
      查 ApplicationChannel(priority 映射) + ModelInstance
      构造 RoutingRequest(含 channelId→priority 映射)
  → RouterChain: Permission(@100) → EndpointHealth(@200) → Priority(@300,排序器) → LoadBalance(@9999,透传)
      产出按应用级 priority 升序的完整候选列表(含 clusterId)
  → ChannelFailoverInvoker(候选内逐个试):
     for 候选:
       if commonCauseFailedClusters.contains(候选.clusterId): 跳过+发事件(commonCauseSkip=true)
       candidateReq = 原始request.copy() → convertRequest(若跨协议) → tune(每候选独立)
       → KeyFailoverInvoker(L0 换 Key)
       失败 → ErrorClassifier 分流:
         NONE(请求级/UNKNOWN)→ 立即抛
         L1(共因)→ 标记 clusterId 共因失效 + 发事件 → 试下一候选
     全耗尽 → L3 抛最后异常
  → (调用链外) FailoverEventListener @EventListener 同步持久化 ← 转移事件
  → 容灾总览页 GET /resilience/events 渲染转移事件流(含共因跳过标记)
```

## Implementation Divergence

实现过程中相对设计决策的偏差记录（以实现为准）：

- 无（设计阶段已确认所有实现细节，build 阶段如遇偏差增量记录于此）

## Testing

见 ID5 测试策略。关键验证：
- 路由链主备转移（补 PriorityRouter 缺陷）
- 调谐每候选独立（补调谐缺陷）
- 共因跳过跨域（Cluster 核心价值）
- 删除回归（L2/ResilienceProfile/会话亲和 移除后行为正确）
- 仿真：供应商级共因故障跨域转移
