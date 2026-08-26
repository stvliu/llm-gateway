# Brainstorm Summary

- Change: simplify-resilience-architecture
- Date: 2026-06-29

## 确认的技术方案

核心架构决策见 OpenSpec design.md D1-D10（已确认）。本文件为实现层面技术细节确认结果：

1. **ApplicationChannel.priority 注入**：方案 A — RoutingRequest 删 resilienceProfile 字段，改为携带 `Map<Long channelId, Integer priority>`。InstanceSelector 构造 RoutingRequest 前查 ApplicationChannel 填充映射。PriorityRouter 从映射取 priority 排序。不污染 ModelInstance 实体。
2. **共因跳过**：RoutingContext record 增加 `clusterId` 字段（RoutingResolver 构造时从 Channel 填充，转移事件发布也省反查）。invoker 内局部 `Set<Long> commonCauseFailedClusters` 标记（天然本次请求有效）。跳过候选发转移事件 `commonCauseSkip=true`；所有剩余候选都被共因跳过时抛 lastException。
3. **调谐下沉**：convertRequest + tune 一起下沉到 ChannelFailoverInvoker。每候选从原始入站 request 调 `ProtocolRequest.copy()` 派生副本，在副本上 convert+tune，不动原始。ProtocolRequest 接口加 `copy()` 方法，OpenAIChatRequest/AnthropicMessagesRequest 各自实现字段拷贝。ChatDispatchServiceImpl 删阶段3/4，传原始 request 给 invoker。
4. **DB 迁移**：V51-V58 全在 master，必须新加迁移从 V59 起：V59 application_channels 加 priority；V60 clusters 删 region/priority/health_status；V61 applications 加 timeout + 删 resilience_profile_id；V62 删 resilience_profiles 表 + seed（依赖 V61）；V63 model_instances 删 priority；V64 failover_events 加 common_cause_skip。
5. **测试策略**：补三类集成测试（经完整路由链，非直接构造候选）——路由链主备转移+调谐正确、共因跳过跨域、删除回归。invoker 单测改注入路由链产出候选。删 L2/ResilienceProfile/会话亲和相关测试。仿真验证共因跨域。

## 关键取舍与风险

- **RoutingContext 加 clusterId**：record 字段扩张，但避免 invoker 反查 DB（转移是失败路径，反查增 DB 压力）。一次填充多处复用（共因跳过 + 转移事件）。
- **ProtocolRequest.copy()**：手写字段拷贝 vs Jackson 深拷贝。选手写避免性能开销与循环引用风险，代价是接口扩张（各实现类实现 copy）。
- **convertRequest 一起下沉**：若不下沉，跨协议候选换渠道后协议转换错误（留缺陷）。下沉后每候选独立 convert+tune。
- **删 resilience_profiles 表不可逆**：V62 删表。生产前确认无应用依赖（ResilienceProfile 上 change 才引入，风险可控）。
- **共因跳过误杀**：标记仅本次请求有效不持久化 + 熔断器 HALF_OPEN 试探保底。折中方案。
- **priority 映射 null 回退**：ApplicationChannel.priority 为 null 时回退默认值（如 100），避免 NPE。

## 测试策略

- **集成测试（补缺口）**：经完整 RouterChain 产出候选，非直接构造 List.of(ctx1,ctx2)
  - 路由链主备转移：主备 priority 不同 → 候选含 [主,备] → 主失败换备
  - 调谐正确：候选不同 upstreamModelName → 换渠道后请求 model 正确
  - 共因跳过跨域：同 clusterId 共因失败 → 跳过同域 → 试异域
  - 删除回归：L1 全耗尽抛最后异常（不进 L2）；UNKNOWN→NONE 不转移；timeout 从 Application 读
- **单元测试**：ErrorClassifier/PriorityRouter 逻辑保留；ChannelFailoverInvokerTest 删 L2 场景，新增共因跳过场景，候选改为路由链产出
- **仿真测试**：provider-simulator 验证供应商级共因故障跨域转移
- **删除测试**：DegradationServiceTest/ResilienceProfileIntegrationTest/SessionAffinityStoreTest 等直接删

## Spec Patch

OpenSpec delta specs 已在 open 阶段创建（10 个）。design 阶段 brainstorming 未发现需大幅回写的 spec 缺口，仅以下小补丁：
- channel-failover spec 的「转移事件发布」Requirement 已含 commonCauseSkip 字段（open 阶段已写入），无需补
- cluster-failover spec 已含 clusterId 共因跳过依据，无需补
- 无需新增验收场景（核心场景 open 阶段已覆盖）

---

## 探索发现（Task 1）

- `RoutingRequest` 当前携带 `resilienceProfile`（Task 4.9 贯穿），删 ResilienceProfile 后此字段退场，可被应用级 priority 映射替代
- `PermissionRouter` 已通过 `applicationChannelGateway.findChannelIdsByApplicationId` 查授权渠道集合，可顺势取 priority（已批量查 ApplicationChannel）
- `RoutingRequest` 已携带 `applicationId`，priority 注入有两种方案待定
- `InstanceSelector.findActiveByModelIdOrderByPriority` 当前按 ModelInstance.priority 排序，需改为应用级 priority

## 待确认实现点

1. ✅ ApplicationChannel.priority 注入点 → **方案 A:RoutingRequest 携带 Map<channelId,priority>**（删 resilienceProfile 字段，InstanceSelector 提前查 ApplicationChannel 填充，PriorityRouter 从映射取 priority 排序。不污染 ModelInstance）
2. ✅ 共因跳过实现细节 → **RoutingContext 增加 clusterId 字段**（RoutingResolver 构造时从 Channel 填充，转移事件发布也省反查）；**跳过发转移事件 commonCauseSkip=true，全跳过抛 lastException**；标记用方法内局部 Set<Long> commonCauseFailedClusters（天然本次请求有效）
3. ✅ 调谐下沉 → **convertRequest + tune 一起下沉到 invoker**；每候选从原始入站 request 调 `ProtocolRequest.copy()` 派生副本，在副本上 convert+tune，不动原始；`ProtocolRequest` 接口加 `copy()` 方法（OpenAIChatRequest/AnthropicMessagesRequest 各自实现字段拷贝）
4. ✅ DB 迁移 → **V51-V58 全在 master，必须新加迁移，从 V59 起**：
   - V59: application_channels 加 priority 列
   - V60: clusters 删 region/priority/health_status 列
   - V61: applications 加 timeout 列 + 删 resilience_profile_id 列
   - V62: 删 resilience_profiles 表 + 删 V56 seed（依赖 V61 先删外键引用）
   - V63: model_instances 删 priority 列
   - V64: failover_events 加 common_cause_skip 列
   - 删 resilience_profiles 表不可逆，生产前确认无应用依赖（ResilienceProfile 上 change 才引入，风险可控）
5. 测试策略（集成测试补缺口）——进行中
