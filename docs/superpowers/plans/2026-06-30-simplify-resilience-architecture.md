# 容灾架构简化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

---
change: simplify-resilience-architecture
design-doc: docs/superpowers/specs/2026-06-30-simplify-resilience-architecture-design.md
base-ref: 17c7c1f3b897aa55ffc88218e0a2844e846b8611
---

**Goal:** 将过度设计的四层容灾栈（L0/L1/L2/L3）+ Cluster 域级聚合收敛为「解决实际问题的最小充分集」——容灾止于通道（L0 换 Key / L1 换渠道 + 共因跳过 / L3 抛错），降级决策还给应用；Cluster 语义改造为跨供应商故障独立性分组并驱动 L1 共因跳过；转移顺序改应用级 `ApplicationChannel.priority`；修复 PriorityRouter 选择器→排序器、调谐下沉每候选独立两个已存缺陷。

**Architecture:** 删除 L2 模型降级层、DomainHealth 路由器、PinnedModel、会话亲和、独立 ResilienceProfile（仅剩 timeout 下沉到 Application）；RouterChain 收敛为 `Permission(@100) → EndpointHealth(@200) → Priority(@300,排序器) → LoadBalance(@9999,透传)`；`ChannelFailoverInvoker` 在候选内逐个试，每候选独立 `convertRequest + tune`，共因失败按 clusterId 跳过同域候选；转移事件流新增 `commonCauseSkip` 标记。依赖顺序：先修两个已存缺陷（L1 正确性前提）→ 应用级 priority → 删 L2/DomainHealth/PinnedModel/会话亲和 → ResilienceProfile 降级 → Cluster 改造与共因跳过 → 前端 → spec 同步 → 全链路回归。

**Tech Stack:** Java 21 + Spring Boot 3.5.x, Spring MVC, JPA/Hibernate, Flyway 迁移, H2(开发)/PostgreSQL(生产), Redis, JUnit 5 + Mockito；前端 React + Vite + Ant Design + Vitest。

## Global Constraints

- COLA Light 5.0 单模块分层：domain 只依赖 Gateway 接口，Gateway 实现在 infrastructure；禁止跨层/反向依赖。
- **领域模型纯洁性铁律**：JPA 实体只含 Getter/Setter，禁止含业务逻辑；priority 是路由上下文数据，**不挂 ModelInstance 领域实体**（ID1）。
- 全实体可审计：每张业务表含 `created_by/created_at/updated_by/updated_at`（继承 `BaseEntity`）。
- 表名 snake_case 复数；主键 `id BIGINT AUTO_INCREMENT`；外键 `*_id BIGINT`。
- public 方法必须中文 Javadoc；类/复杂逻辑中文注释；业务逻辑块中文解释。
- 双 API 兼容不可破坏：`/v1/chat/completions` 与 `/v1/messages` 行为保持。
- 不重写上游客户端与重试/熔断算法本身（复用 `ResilientUpstreamClient`/`CircuitBreaker`/`KeyFailoverInvoker`）。
- 所有可变参数走 `@ConfigurationProperties`，禁止魔法数字。
- 构建命令：`./mvnw -pl gateway-boot -am test`（后端测试）；`cd gateway-console && npm run build`（前端构建）。
- Flyway 版本号续接现有最大 V58，本计划从 **V59** 起（见 File Structure 迁移表）。实现时以仓库实际最大版本号 +1 为准。
- 删 `resilience_profiles` 表不可逆：生产前确认无应用依赖（ResilienceProfile 由前一 change 才引入，预期无生产数据，风险可控）。
- 涉及业务逻辑的任务遵循 TDD：先写失败测试，再实现，再跑绿，再 commit。
- **tasks.md 同步铁律**：每完成一个 tasks.md 子项的验收后立即勾选对应 `- [x]`，每个分组（1-12）完成后一次 commit，commit message 体现设计意图。

## File Structure

### 领域层修改

- **Modify** `domain/supply/valueobject/RoutingContext.java` — record 新增 `clusterId` 字段（ID2，由 RoutingResolver 从 `Channel.clusterId` 填充）
- **Modify** `domain/protocol/contract/ProtocolRequest.java` — 接口新增 `copy()` 方法（ID3）
- **Modify** `domain/protocol/contract/OpenAIChatRequest.java` — 实现 `copy()` 手写字段拷贝
- **Modify** `domain/protocol/contract/AnthropicMessagesRequest.java` — 实现 `copy()` 手写字段拷贝
- **Modify** `domain/application/entity/ApplicationChannel.java` — 加 `priority` 字段 + Getter/Setter（ID1）
- **Modify** `domain/application/entity/Application.java` — 加 `timeout` 字段，删 `resilienceProfileId`（Task 8）
- **Modify** `domain/resilience/entity/Cluster.java` — 删 `region`/`priority`/`healthStatus`，保留 code/name/description + 审计；Javadoc 改语义为「跨供应商故障独立性分组」（Task 6）
- **Delete** `domain/resilience/entity/ResilienceProfile.java`、`FailoverEvent` 的 L2 相关字段保留、`ClusterHealthStatus` 枚举（若 Cluster 不再用）
- **Delete** `domain/resilience/entity/L2DegradationRequiredException.java`
- **Modify** `domain/supply/enums/FailoverDecision.java` — 删 `L2` 枚举值
- **Modify** `domain/resilience/entity/FailoverEvent.java`（与 DO）— clusterId 保留，新增 `commonCauseSkip` 标记
- **Modify** `common/event/FailoverOccurredEvent.java` — 新增 `commonCauseSkip` 字段

### 应用层修改

- **Modify** `application/proxy/routing/PriorityRouter.java` — `filter` 改排序器：按应用级 priority 升序输出完整列表，不收敛；`isForce` 语义调整（Task 1）
- **Modify** `application/proxy/routing/RoutingRequest.java` — 删 `resilienceProfile`，改为携带 `Map<Long channelId, Integer priority>`（ID1）
- **Modify** `application/proxy/routing/InstanceSelector.java` — 构造 RoutingRequest 前查 ApplicationChannel priority 构建映射；`findActiveByModelIdOrderByPriority` 适配应用级排序
- **Modify** `application/proxy/routing/RouterChain.java` — 顺序变为 `Permission→EndpointHealth→Priority→LoadBalance`（删 ClusterAffinityRouter/PinnedModelRouter）
- **Modify** `application/proxy/RoutingResolver.java` — 构造 RoutingContext 时填充 `clusterId`
- **Modify** `application/proxy/ChatDispatchServiceImpl.java` — 删阶段3(convertRequest)/阶段4(tune)/L2 重路由循环，传原始 request 给 invoker；profile 解析改读 Application.timeout
- **Modify** `application/proxy/invoker/ChannelFailoverInvoker.java` — 注入 `OutboundTuner`+`ProtocolConverter`，每候选独立 convert+tune（基于 `request.copy()`）；删 `tryL2Degradation`/`degradationService`；实现共因跳过（ID2）；`publishFailoverEvent` 新增 commonCauseSkip，clusterId 从 RoutingContext 直取（删 ChannelGateway 反查）
- **Modify** `application/proxy/failover/ErrorClassifier.java` — UNKNOWN→NONE，`getOrDefault` 兜底改 NONE
- **Delete** `application/degradation/` 整包（DegradationService/Impl/Properties/Event/RecoveredEvent + `@Scheduled recoveryCheck`）
- **Delete** `application/proxy/routing/ClusterAffinityRouter.java`、`ClusterHealthAggregator.java`、`PinnedModelRouter.java`
- **Delete** `application/resilience/ResilienceResolver.java`、`ResilienceProfileApplier.java`
- **Delete** `infrastructure/resilience/affinity/`（RedisSessionAffinityStore/InMemorySessionAffinityStore）+ `SessionAffinityConfig`、`SessionAffinityStore` 接口

### 基础设施层修改

- **Modify** `infrastructure/application/gateway/ApplicationChannelGatewayImpl.java` + DO + Repository — 适配 priority
- **Modify** `infrastructure/resilience/gateway/ClusterGatewayImpl.java` + DO + Repository — 删 region/priority/healthStatus
- **Delete** `infrastructure/resilience/gateway/ResilienceProfileGatewayImpl.java` + DO + Repository
- **Modify** `infrastructure/resilience/gateway/FailoverEventGatewayImpl.java` + DO — commonCauseSkip 列
- **Create** Flyway 迁移 V59-V64（见下表）

### 适配层修改

- **Delete** `adapter/api/ResilienceProfileController.java` + DTO
- **Modify** `adapter/api/ClusterController.java` — 字段瘦身，路径 `/api/v1/resilience/clusters` 保留
- **Modify** `adapter/api/ApplicationController.java` + `ApplicationServiceImpl` — timeout CRUD，移除 `/applications/{id}/resilience` 端点

### Flyway 迁移表（ID4）

| 版本 | 变更 | 依赖 |
|------|------|------|
| `V59__add_application_channel_priority.sql` | application_channels 加 `priority INT NULL` | - |
| `V60__drop_cluster_extra_columns.sql` | clusters 删 region/priority/health_status 列 | - |
| `V61__application_timeout_drop_profile_fk.sql` | applications 加 timeout 列、删 resilience_profile_id 列 | - |
| `V62__drop_resilience_profiles.sql` | 删 resilience_profiles 表 + 删 V56 seed 数据 | V61（先删外键引用） |
| `V63__drop_model_instance_priority.sql` | model_instances 删 priority 列 | V59 |
| `V64__add_failover_event_common_cause_skip.sql` | failover_events 加 `common_cause_skip BOOLEAN DEFAULT FALSE` | - |

### 前端（gateway-console）

- **Modify** 容灾总览页：clusterId 保留，新增共因跳过展示，移除降级/会话亲和/PinnedModel
- **Delete** 画像模板页（随 ResilienceProfile 退场）
- **Modify** Applications 页：移除容灾画像绑定，加 timeout 配置 + 渠道 priority 排序
- **Modify** Channels 页 + types/services/locales：Cluster 字段瘦身（删 region/priority/healthStatus）

---

## Task 1: 修复 PriorityRouter 选择器→排序器（已存缺陷，L1 前置）

**Files:**
- Modify: `application/proxy/routing/PriorityRouter.java`
- Test: `application/proxy/routing/RouterChainTest.java`、`application/proxy/routing/PriorityRouterTest.java`（若无则新建）

**Interfaces:**
- Consumes: `RoutingRequest.getChannelPriorityMap()` → `Map<Long, Integer>`（Task 3 提供；本任务先用 `ModelInstance.priority` 临时排序，Task 3 切换）
- Produces: `PriorityRouter.filter(...)` 返回按 priority 升序的**完整**候选列表，不收敛

**背景**：当前 `PriorityRouter.filter` 在 `isForce=true` 时收敛到最优 priority 组，导致 L1 换不到备渠道。需改为排序器：输出完整列表按 priority 升序。

- [ ] **1.1 grep 确认 `ModelInstance.priority` 的所有用途**（负载均衡/监控/前端展示），记录是否可删。Run: `grep -rn "getPriority\|\.priority" gateway-boot/src/main/java/com/codingas/gateway/domain/model gateway-boot/src/main/java/com/codingas/gateway/application`。结论记录到本 plan 的 Implementation Divergence 或 commit body。
- [ ] **1.2 写失败测试：主备 priority 不同时输出完整列表 [主,备] 不丢备**。在 `PriorityRouterTest`（或 `RouterChainTest`）新增：构造 priority=1(主)、priority=2(备) 两个 ModelInstance，断言 `filter` 返回 size==2 且顺序为 [主,备]。当前实现会收敛到 [主]，测试应 FAIL。
- [ ] **1.3 写失败测试：RouterChain 经 PriorityRouter 后候选列表含全部 priority 组**。在 `RouterChainTest` 新增经完整链路的断言。
- [ ] **1.4 改 `PriorityRouter.filter` 为排序器**：按 priority 升序输出完整列表，不收敛；调整 `isForce` 语义（force 时仍排序不收敛）。移除「收敛到最优组」分支。
- [ ] **1.5 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test -Dtest=PriorityRouterTest,RouterChainTest`，再 `./mvnw -pl gateway-boot -am test`。
- [ ] **1.6 commit**：`fix(resilience): PriorityRouter 改为排序器，主备 priority 不丢备`

---

## Task 2: 修复调谐下沉 invoker，每候选独立（已存缺陷，L1 前置）

**Files:**
- Modify: `domain/protocol/contract/ProtocolRequest.java`、`OpenAIChatRequest.java`、`AnthropicMessagesRequest.java`
- Modify: `application/proxy/invoker/ChannelFailoverInvoker.java`（注入 `OutboundTuner`+`ProtocolConverter`）
- Modify: `application/proxy/ChatDispatchServiceImpl.java`（删阶段3/4对外调谐）
- Test: `application/proxy/invoker/ChannelFailoverInvokerTest.java`、`integration/ChannelFailoverIntegrationTest.java`

**Interfaces:**
- Produces: `ProtocolRequest.copy()` → 同类型副本（手写字段拷贝）；`ChannelFailoverInvoker.invoke/invokeStream` 每候选基于 `originalRequest.copy()` → `convertRequest`(若跨协议) → `tune` → `keyFailoverInvoker.invoke`

- [ ] **2.1 写失败测试：候选不同 upstreamModelName 时，L1 换渠道后请求 model 正确**。构造两候选 upstreamModelName 不同，主候选失败，断言备候选收到的 request.getModel()==备候选的 upstreamModelName。当前只对首项调谐，测试应 FAIL。
- [ ] **2.2 `ProtocolRequest` 接口加 `copy()`**，`OpenAIChatRequest`/`AnthropicMessagesRequest` 各自实现手写字段拷贝（不用 Jackson 深拷贝）。
- [ ] **2.3 `ChannelFailoverInvoker` 注入 `OutboundTuner` + `ProtocolConverter`**，`invoke`/`invokeStream` 内每候选试之前：`candidateReq = originalRequest.copy()` → 若 `needsProtocolAdaptation()` 则 `convertRequest(candidateReq, candidate)` → `outboundTuner.tune(candidateReq, candidate)` → `keyFailoverInvoker.invoke(candidate, candidateReq)`。
- [ ] **2.4 `ChatDispatchServiceImpl` 删阶段3(convertRequest)与阶段4(tune)**，传**原始入站 request** 给 invoker（dispatch 与 dispatchStream 同步改）。
- [ ] **2.5 评估 `convertRequest` 跨协议转换是否需每候选独立**：本任务已下沉，确认流式 `invokeStream` 的 delegateCallback 协议转换逻辑不受影响（首字节后转换仍由 callback 完成）。
- [ ] **2.6 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **2.7 commit**：`fix(resilience): 调谐下沉 invoker，每候选独立 convert+tune`

---

## Task 3: 应用级 ApplicationChannel.priority 取代全局（依赖 Task 1）

**Files:**
- Modify: `domain/application/entity/ApplicationChannel.java`
- Modify: `application/proxy/routing/RoutingRequest.java`、`InstanceSelector.java`、`PriorityRouter.java`
- Modify: `infrastructure/application/gateway/ApplicationChannelGatewayImpl.java` + DO + Repository
- Create: `db/migration/V59__add_application_channel_priority.sql`
- Test: `application/proxy/routing/PriorityRouterTest.java`、`InstanceSelectorTest.java`

**Interfaces:**
- `RoutingRequest` 删 `resilienceProfile`（Task 8 才彻底删引用，本任务先加 `channelPriorityMap` 字段），新增 `Map<Long channelId, Integer priority> getChannelPriorityMap()`
- `ApplicationChannelGateway.findByApplicationId(Long)` → `List<ApplicationChannel>`（含 priority）
- `PriorityRouter` 排序键改 `map.get(mi.getChannelId())`，null 回退默认值 100

- [ ] **3.1 `ApplicationChannel` 实体加 `priority` 字段**（Integer，可空）+ Getter/Setter。
- [ ] **3.2 Flyway V59**：`ALTER TABLE application_channels ADD COLUMN priority INT NULL;`
- [ ] **3.3 `ApplicationChannelGateway`/Impl/DO/Repository 适配 priority**（查询/持久化映射）。
- [ ] **3.4 `InstanceSelector.select` 构造 RoutingRequest 前**查 `ApplicationChannelGateway.findByApplicationId(applicationId)` 取该应用所有授权渠道 priority，构建 `Map<channelId, priority>` 填入 RoutingRequest。`PermissionRouter` 不变（仍按 channelId 过滤）。
- [ ] **3.5 `PriorityRouter` 排序键改 ApplicationChannel.priority**：从 `request.getChannelPriorityMap()` 取映射，按 `map.get(mi.getChannelId())` 升序排序，null 回退 100。
- [ ] **3.6 `InstanceSelector.findActiveByModelIdOrderByPriority` 适配应用级排序**（不再依赖 ModelInstance.priority）。
- [ ] **3.7 写测试：同渠道对不同应用不同 priority，各自转移顺序独立**。
- [ ] **3.8 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **3.9 commit**：`feat(resilience): 转移顺序改应用级 ApplicationChannel.priority`

---

## Task 4: 删除 L2 模型降级层（独立）

**Files:**
- Delete: `application/degradation/` 整包、`domain/resilience/entity/L2DegradationRequiredException.java`
- Modify: `application/proxy/invoker/ChannelFailoverInvoker.java`、`ChatDispatchServiceImpl.java`、`failover/ErrorClassifier.java`、`domain/supply/enums/FailoverDecision.java`
- Test: Delete `DegradationServiceTest`；适配 `ChannelFailoverInvokerTest`/`ChatDispatchServiceTest`/`ChannelFailoverIntegrationTest`

**Interfaces:**
- `FailoverDecision` 删 `L2`（仅留 L1/NONE）
- `ErrorClassifier.classify(UNKNOWN_ERROR)` → NONE；`getOrDefault` 兜底改 NONE
- `ChannelFailoverInvoker` 候选耗尽直接抛 lastException，签名去 profile 参数

- [ ] **4.1 整删 `application/degradation/` 包**（DegradationService/Impl/Properties/Event/RecoveredEvent + `@Scheduled recoveryCheck`）。
- [ ] **4.2 整删 `L2DegradationRequiredException`**。
- [ ] **4.3 `ChannelFailoverInvoker` 删 `tryL2Degradation`/`degradationService` 字段/构造参数**，候选耗尽直接抛 lastException；`invoke`/`invokeStream` 签名去 `profile` 参数。
- [ ] **4.4 `ChatDispatchServiceImpl` 删** `invokeWithL2Failover`/`invokeStreamWithL2Failover`/`resolveMaxDepth`/`unwrapL2Cause`/`MAX_DEGRADATION_DEPTH`/`resolveProfileSafely`/`resilienceResolver` 依赖，直接调 invoker。
- [ ] **4.5 `FailoverDecision` 删 L2 枚举值**。
- [ ] **4.6 `ErrorClassifier` UNKNOWN→NONE**，`getOrDefault` 兜底改 NONE（删 L2 映射）。
- [ ] **4.7 删 `DegradationServiceTest`**，适配 `ChannelFailoverInvokerTest`/`ChatDispatchServiceTest`/`ChannelFailoverIntegrationTest`（删 L2 场景）。
- [ ] **4.8 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **4.9 commit**：`refactor(resilience): 删除 L2 模型降级层，降级决策还给应用`

---

## Task 5: 删除 DomainHealth 路由器（独立，与 6 无强依赖）

**Files:**
- Delete: `application/proxy/routing/ClusterHealthAggregator.java`、`ClusterAffinityRouter.java`、`domain/resilience/entity/ClusterHealthStatus.java`（若 Cluster 不再用）
- Modify: `application/proxy/routing/RouterChain.java`
- Test: `RouterChainTest`、`HealthRouterTest`

- [ ] **5.1 整删 `ClusterHealthAggregator`**。
- [ ] **5.2 整删 `ClusterAffinityRouter`**，RouterChain 顺序变为 `Permission→EndpointHealth→Priority→LoadBalance`。
- [ ] **5.3 删 `ClusterHealthStatus` 枚举**（确认 Cluster 实体已不引用，与 Task 6 协调；若 6 未删 healthStatus 字段则保留到 6）。
- [ ] **5.4 适配 `RouterChainTest`/`HealthRouterTest`**（删域级聚合断言）。
- [ ] **5.5 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **5.6 commit**：`refactor(resilience): 删除 DomainHealth 域级聚合路由器`

---

## Task 6: Cluster 语义改造 + 瘦身字段（独立，保留实体名）

**Files:**
- Modify: `domain/resilience/entity/Cluster.java`、`ClusterGateway`/Impl/DO/Repository、`ClusterController`
- Create: `db/migration/V60__drop_cluster_extra_columns.sql`
- Modify: `common/event/FailoverOccurredEvent.java`、`domain/resilience/entity/FailoverEvent.java` + DO
- Test: 适配所有 Cluster 引用测试

**Interfaces:**
- `Cluster` 仅留 code/name/description + 审计；Javadoc 改「跨供应商故障独立性分组」
- `FailoverOccurredEvent`/`FailoverEvent`/DO 的 clusterId 保留，新增 `commonCauseSkip` 标记（Task 9 使用）

- [ ] **6.1 `Cluster` 实体删 region/priority/healthStatus 字段**，保留 code/name/description + 审计；更新 Javadoc 语义为「跨供应商故障独立性分组」。
- [ ] **6.2 `Channel.clusterId` 字段名保留不变**（仅确认，不改）。
- [ ] **6.3 `ClusterGateway`/Impl/DO/Repository 删 region/priority/healthStatus 适配**。
- [ ] **6.4 Flyway V60**：`ALTER TABLE clusters DROP COLUMN region, DROP COLUMN priority, DROP COLUMN health_status;`（确认列存在后再删，H2/PG 兼容）。
- [ ] **6.5 `ClusterController` 适配**（字段瘦身，API 路径 `/api/v1/resilience/clusters` 保留）。
- [ ] **6.6 `ChannelFailoverInvoker.publishFailoverEvent` 的 clusterId 反查逻辑保留**（Task 9 改为从 RoutingContext 直取，本任务先保留语义不变）。
- [ ] **6.7 `FailoverOccurredEvent`/`FailoverEvent`/DO 的 clusterId 字段保留**，新增 `commonCauseSkip` 标记字段（boolean，默认 false）+ Flyway V64 列。本任务先加字段，Task 9 填充逻辑。
- [ ] **6.8 适配所有 Cluster 引用测试**（删 region/priority/healthStatus 相关断言）。
- [ ] **6.9 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **6.10 commit**：`refactor(resilience): Cluster 语义改造为跨供应商故障独立性分组并瘦身字段`

---

## Task 7: 删除 PinnedModel 与会话亲和（独立）

**Files:**
- Delete: `application/proxy/routing/PinnedModelRouter.java`、`infrastructure/resilience/affinity/`（Redis/InMemory 双实现）+ `SessionAffinityConfig`、`SessionAffinityStore` 接口
- Modify: `domain/resilience/entity/ResilienceProfile.java`（删 enablePinnedModel/pinnedModelId/enableSessionAffinity/sessionAffinityTtlMinutes，若 Task 8 未先删实体）
- Test: 适配测试，删 `SessionAffinityStoreTest`

- [ ] **7.1 整删 `PinnedModelRouter`**。
- [ ] **7.2 删 `ResilienceProfile.enablePinnedModel`/`pinnedModelId`**（若 8 未先删实体）。
- [ ] **7.3 整删 `SessionAffinityStore`**（Redis/InMemory 双实现）+ `SessionAffinityConfig`。
- [ ] **7.4 删 `ResilienceProfile.enableSessionAffinity`/`sessionAffinityTtlMinutes`**。
- [ ] **7.5 RouterChain 去除 PinnedModel**（已在 5.2 处理，此处 grep 确认无残留）。
- [ ] **7.6 适配测试**，删 `SessionAffinityStoreTest`。
- [ ] **7.7 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **7.8 commit**：`refactor(resilience): 删除 PinnedModel 与会话亲和`

---

## Task 8: ResilienceProfile 实体降级（依赖 Task 4/7）

**Files:**
- Modify: `domain/application/entity/Application.java`
- Create: `db/migration/V61__application_timeout_drop_profile_fk.sql`、`V62__drop_resilience_profiles.sql`、`V63__drop_model_instance_priority.sql`
- Delete: `domain/resilience/entity/ResilienceProfile.java`、`ResilienceProfileGateway`/Impl、`ResilienceResolver`、`ResilienceProfileApplier`、`adapter/api/ResilienceProfileController.java` + DTO
- Modify: `ChatDispatchServiceImpl.java`、`InstanceSelector.java`、`ApplicationServiceImpl`/Controller
- Test: 适配/重写 `ResilienceProfileIntegrationTest` 等

**Interfaces:**
- `Application` 加 `timeout` 字段，删 `resilienceProfileId`
- profile 解析改为直接读 `Application.timeout`；`RoutingRequest` 彻底删 `resilienceProfile` 字段及构造参数

- [ ] **8.1 `Application` 实体加 `timeout` 字段**，删 `resilienceProfileId`。
- [ ] **8.2 Flyway V61**：applications 加 timeout 列、删 resilience_profile_id 列。**V62**：删 resilience_profiles 表 + 删 V56 seed 数据（V61 先于 V62，外键约束）。**V63**：model_instances 删 priority 列（依赖 V59 priority 改应用级）。
- [ ] **8.3 整删 `ResilienceProfile` 实体、`ResilienceProfileGateway`/Impl、`ResilienceResolver`、`ResilienceProfileApplier`**。
- [ ] **8.4 整删 `ResilienceProfileController` + DTO**。
- [ ] **8.5 `ChatDispatchServiceImpl`/`InstanceSelector` 的 profile 解析改为直接读 Application.timeout**；`RoutingRequest` 删 `resilienceProfile` 字段及所有构造参数。
- [ ] **8.6 `ApplicationServiceImpl`/Controller 适配 timeout CRUD**，移除 `/applications/{id}/resilience` 端点。
- [ ] **8.7 适配 `ResilienceProfileIntegrationTest` 等测试**（重写为 timeout 读取场景或删除两对照场景）。
- [ ] **8.8 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **8.9 commit**：`refactor(resilience): ResilienceProfile 退场，timeout 下沉到 Application`

---

## Task 9: L1 clusterId 共因跳过（依赖 Task 1+6）

**Files:**
- Modify: `domain/supply/valueobject/RoutingContext.java`（加 clusterId，ID2）
- Modify: `application/proxy/RoutingResolver.java`（填充 clusterId）
- Modify: `application/proxy/invoker/ChannelFailoverInvoker.java`（共因跳过逻辑 + publishFailoverEvent 改用 RoutingContext.clusterId）
- Test: `ChannelFailoverInvokerTest`、`integration/ChannelFailoverIntegrationTest`、仿真测试

**Interfaces:**
- `RoutingContext` 新增 `Long clusterId` 字段
- `ChannelFailoverInvoker` 局部 `Set<Long> commonCauseFailedClusters`：共因失败标记 clusterId → 跳过同域候选

- [ ] **9.1 写失败测试：同 clusterId 共因失败时，L1 跳过同域候选试异域**。构造同 clusterId 两候选 + 异域一候选，主候选共因失败，断言同域第二候选被跳过、异域候选被试。
- [ ] **9.2 写测试：共因跳过标记仅本次请求有效，下次请求不继承**（验证局部 Set，无持久化）。
- [ ] **9.3 写测试：非共因失败（NONE）不触发共因跳过**（INVALID_REQUEST 直接抛，不标记）。
- [ ] **9.4 `RoutingContext` record 加 `clusterId` 字段**；`RoutingResolver` 构造时从 `Channel.clusterId` 填充。
- [ ] **9.5 `ChannelFailoverInvoker.invoke/invokeStream` 实现共因跳过**：试候选前若 `commonCauseFailedClusters.contains(candidate.clusterId())` 跳过并发转移事件（`commonCauseSkip=true`），不计入 lastException；候选共因失败（`FailoverDecision=L1`）时 `add(candidate.clusterId())` 发事件（`commonCauseSkip=false`）记录 lastException；全耗尽抛 lastException。
- [ ] **9.6 `publishFailoverEvent` 新增「是否共因跳过」标记**，clusterId 从 RoutingContext 直取（删 `resolveClusterId`/ChannelGateway 反查）。
- [ ] **9.7 端到端集成测试：故障域级共因故障→L1 跳过同域→跨域转移成功**。
- [ ] **9.8 跑绿 + 回归**：`./mvnw -pl gateway-boot -am test`
- [ ] **9.9 commit**：`feat(resilience): L1 clusterId 共因跳过跨域转移`

---

## Task 10: 前端适配（gateway-console）

**Files:**
- Modify: 容灾总览页、Applications 页、Channels 页、types/services/locales
- Delete: 画像模板页

- [ ] **10.1 容灾总览页**：clusterId 字段保留（语义随 Cluster 改造），新增共因跳过展示，移除降级/会话亲和/PinnedModel。
- [ ] **10.2 画像模板页整删**（随 ResilienceProfile 退场）。
- [ ] **10.3 Applications 页**：移除容灾画像绑定，加 timeout 配置 + 渠道 priority 排序。
- [ ] **10.4 Channels 页**：clusters→clusters，移除「紧切域」（依赖已删域级路由）。
- [ ] **10.5 types/services/locales 适配**：Cluster 字段瘦身（删 region/priority/healthStatus，删 L2/Pinned/会话亲和相关）。
- [ ] **10.6 `cd gateway-console && npm run build` 通过**（必要时 `npx vitest run` 跑前端测试）。
- [ ] **10.7 commit**：`feat(console): 适配容灾架构简化（Cluster 瘦身/共因跳过/删画像）`

---

## Task 11: spec 同步与文档

**Files:**
- Modify: `openspec/changes/simplify-resilience-architecture/specs/*/spec.md`（delta）
- Modify: `doc/容灾方案设计.md`、`doc/容灾管理范式.md`

- [ ] **11.1 确认 delta specs 与实现一致**（本 change 已创建 10 个 delta specs，逐个核对）。
- [ ] **11.2 更新 `doc/容灾方案设计.md` / `doc/容灾管理范式.md`**：四层→三层，Cluster 语义改造+瘦身字段，删 L2/DomainHealth/PinnedModel/会话亲和。
- [ ] **11.3 grep 确认无残留**：`L2/DegradationService/PinnedModel/SessionAffinity/ResilienceProfile/ClusterHealthAggregator/ClusterAffinityRouter/ModelInstance.priority` 相关引用清除。
- [ ] **11.4 commit**：`docs(resilience): 同步容灾架构简化 spec 与设计文档`

---

## Task 12: 全链路回归

- [ ] **12.1 `./mvnw -pl gateway-boot -am test` 全绿**。
- [ ] **12.2 `cd gateway-console && npm run build` 通过**。
- [ ] **12.3 端到端验证**：主备 priority L1 转移、共因跳过跨域转移、调谐每候选独立、INVALID_REQUEST 不转移。
- [ ] **12.4 commit**（如有修复）。

---

## Self-Review

**1. Spec coverage**：tasks.md 12 分组全部映射到 Task 1-12，design ID1-ID5 均有对应任务（ID1→Task 3，ID2→Task 9，ID3→Task 2，ID4→File Structure 迁移表 V59-V64，ID5→Task 1/2/9 测试策略）。Goals 五项（三层收敛/Cluster 语义/应用级 priority/修两缺陷/裁剪五件）全覆盖。

**2. Placeholder scan**：无 TBD/TODO；每任务含具体文件路径、接口签名、TDD 步骤与验收命令。

**3. Type consistency**：`RoutingContext.clusterId`（Task 9 定义）被 `ChannelFailoverInvoker` 共因跳过与 `publishFailoverEvent` 使用（Task 9/6 一致）；`ProtocolRequest.copy()`（Task 2 定义）被 invoker 每候选派生使用；`FailoverDecision` 删 L2（Task 4）后 `ErrorClassifier` 与 invoker 仅引用 L1/NONE（Task 4/9 一致）；`RoutingRequest.getChannelPriorityMap()`（Task 3 定义）被 `PriorityRouter` 使用（Task 1/3 一致）。依赖顺序与 design.md 一致：1→2→3→4/5/7→8→6/9→10→11→12。

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-30-simplify-resilience-architecture.md`。

按 comet-build 流程，接下来需由用户在 plan-ready 暂停点选择继续或暂停，并在继续后选择工作区隔离方式、执行方式、TDD 模式与代码审查模式。
