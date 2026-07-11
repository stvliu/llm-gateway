---
title: 容灾方案
description: 熔断重试与故障转移
---

# 容灾方案设计（应用级失败处理策略 + Channel 级失败转移）

> 状态：已落地，本文档为容灾架构的最终设计文档，与实现对齐。
> 关联：supply 域「Channel 级运行时失败转移」+ 应用级失败处理策略（failureStrategy）+ 应用级超时与渠道优先级 + 端点级熔断器。
> 实现参考：delta spec `openspec/changes/refactor-resilience-to-application-strategy/specs/`（本 change 对容灾架构做了重构：删除 Cluster 故障域实体与共因跳过、引入应用级失败处理策略 FAIL_FAST/FAIL_RETRY/FAIL_OVER 三选一、容灾走向由 Application 策略 + ApplicationChannel.priority + 端点级熔断器承担）。
> 历史脉络：初版由 `resilience-architecture` change 落地四层容灾栈；`simplify-resilience-architecture` change 删 L2 与域级聚合等复杂度；`refactor-resilience-to-application-strategy` change 删 Cluster 故障域与共因跳过，引入应用级失败处理策略，容灾配置收敛到 Application。

## 一、背景与现状诊断

现有供给域层次：

```
Provider ─┬─ Channel ─┬─ ChannelEndpoint（URL + protocol）
          │           └─ ChannelCredential（多个 API Key）
          │   │
          │   └─ ModelInstance（Channel × Model，带 weight/state；priority 字段保留但已不决定转移顺序，见 follow-up）
          │
          └─ Model（规格，如 gpt-4o）
```

调用方层次：`UserApiKey → Application → ApplicationChannel → Channel`，权限锚点为「应用」。Application 是权限+行为双聚合根（Key 归属 + 渠道可见性 + 应用级超时 + 应用级失败处理策略）。

### 容灾栈层级（三层，L0/L1 由应用策略控制）

| 层级 | 组件 | 转移粒度 | 触发 | 策略控制 |
|------|------|---------|------|---------|
| L0 Key 级 | `KeyFailoverInvoker` | 同 Channel 换 Credential | 单 Key 调用抛 `ProviderException` | `FAIL_FAST` 不跑；`FAIL_RETRY`/`FAIL_OVER` 跑 |
| L1 Channel 级 | `ChannelFailoverInvoker` | 同模型换 Channel（按 priority 顺序） | 候选调用失败按错误分流决策 | 仅 `FAIL_OVER` 跑 |
| 路由级 | `RouterChain` | 初始选实例时按权限/健康/优先级过滤 | 仅首次选路，产出候选列表 | — |
| L3 兜底 | — | 抛错给客户端 | L1 全耗尽 / `FAIL_RETRY` 同渠道 Key 耗尽 | — |

> 本 change 删除了 L2 模型降级层与 Cluster 故障域/共因跳过。降级是「有损的最后手段」，覆盖的模型能力问题大多能被请求阶段能力匹配前置消解；降级链人工预配置、有损、静默，与「下游是应用而非人」「Token 成本透明」原则冲突。**容灾止于通道（L0/L1），降级决策还给应用**——应用如需换模型能力，由应用自身决定，不依赖网关运行时降级。共因跳过删除：跨供应商共因渠道首次故障多试一次，由端点级熔断器在连续失败后 OPEN 跳过，避免误杀不共因候选（同供应商不同账户 Key）。

### 已修正的根因性错配

1. **路由器顺序倒置（已修正）**：原 `RouterChain` 为 `Permission → Priority → Health → LoadBalance`，Priority 先于 Health 且 `PriorityRouter` 是 `isForce=true` 只保留 priority 最小组。后果：最小 priority 组被熔断过滤光后直接 force 返回空，次优先级健康渠道永无机会。已修正为 `Permission(@100) → EndpointHealth(@200) → Priority(@300) → LoadBalance(@9999)`，且 `PriorityRouter` 改为排序器（输出完整候选列表不收敛）。

2. **熔断 key 不一致（已修正）**：`HealthRouter` 原用 `channelId`，`KeyFailoverInvoker` 用 `endpointId`。已统一为 `endpointId`：`HealthRouter` 按入站协议经 `EndpointResolver` 从 `channelId` 派生 `endpointId` 后查 `ChannelEndpointCircuitBreakerManager`，与 `KeyFailoverInvoker`（用 `RoutingContext.channelEndpointId()`）共享同一 manager bean（运行时派生方案，不加 DB 字段）。

3. **错误分流不分 reason（已修正）**：引入 `ErrorClassifier` 错误分流表按 `ProviderErrorType` 映射 `FailoverDecision`（L1/NONE），指导转移决策。

### 本 change 删除的复杂度

- **L2 模型降级层**：`DegradationService`/`DegradationServiceImpl`/`DegradationProperties`/`DegradationEvent`/`DegradationRecoveredEvent`/`L2DegradationRequiredException` 整删，`@Scheduled recoveryCheck` 删除。`FailoverDecision` 枚举收敛为 L1/NONE（删 L2）。
- **Cluster 故障域实体与共因跳过**：`Cluster` 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service 整删，`clusters` 表删除；`Channel.clusterId` 字段删除；`RoutingContext.clusterId` 删除；`ChannelFailoverInvoker` 共因跳过逻辑（`commonCauseFailedClusters` + 跳过判定）删除；`FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段删除；`ClusterHealthAggregator`/`ClusterHealthStatus`/`ClusterAffinityRouter` 整删（前序 change 已删，本 change 清理残留）。整域故障靠端点级熔断器自然收敛。
- **PinnedModel + 会话亲和**（前序 change 已删）：`PinnedModelRouter`、`SessionAffinityStore`/`SessionAffinityConfig` 整删。LLM 调用多为无状态，模型锁定由应用侧选择模型实现。
- **ResilienceProfile 实体**（前序 change 已删）：删 L2/PinnedModel/会话亲和后只剩 `timeout` 一个字段，不配独立实体。`ResilienceProfile`/`ResilienceProfileGateway`/Impl/`ResilienceProfileController`/`ResilienceProfileApplier`/`ResilienceResolver`/`resilience_profiles` 表/`resilience_profile_id` 列/`PUT /applications/{id}/resilience` 端点整删。`timeout` 直接挂 `Application` 字段，`failureStrategy` 同样轻量挂 `Application`。

## 二、容灾目标：三层容灾栈 + 应用级策略

```
L0 Key 级      同 Channel 换 Credential              ← 处理单 Key 失效（FAIL_RETRY/FAIL_OVER 跑）
L1 Channel 级  同模型换 Channel（按 priority 顺序）   ← 本方案核心，对用户透明（仅 FAIL_OVER 跑）
L3 兜底        抛错给客户端
```

核心原则：**容灾走向由 Application 决定**——授权哪些渠道（`ApplicationChannel`）+ 转移顺序（`priority`）+ 失败处理策略（`failureStrategy`）+ 超时（`timeout`）。L1 是 Channel 级失败转移的核心，按 `ApplicationChannel.priority` 顺序逐个尝试。共因渠道首次故障多试一次的代价可接受，由端点级熔断器在连续失败后 OPEN 跳过。

> 关于「同一供应商下模型级失败转移是否有必要」：作为容灾手段**无必要**——AUTH/QUOTA/账号限流/网络/宕机都是共因故障，换同供应商模型无效。模型级降级的真正价值是「能力降级兜底」，应由应用按自身场景决定（客服场景救命，CodeX 场景有害），而非网关全局兜底。故 L2 删除，降级决策还给应用。

## 三、应用级失败处理策略 failureStrategy

`Application.failureStrategy` 为 `FailureStrategy` 枚举（`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER` 三选一互斥），控制 `ChannelFailoverInvoker` 的 L0/L1 行为。轻量单字段挂 `Application`，不演变为已删的 `ResilienceProfile` 独立实体。

### 策略与 L0/L1 行为

递进关系：`FAIL_FAST` ⊂ `FAIL_RETRY` ⊂ `FAIL_OVER`（L0/L1 逐级启用）。

| 策略 | L0 同渠道换 Key | L1 换渠道 | 行为 | 适用场景 |
|------|----------------|----------|------|---------|
| `FAIL_FAST`（快速失败） | 否 | 否 | 首个 Key 失败立即抛错（只试首个 Key） | BI 报表（快速失败，应用自身换模型重试） |
| `FAIL_RETRY`（失败重试，默认） | 是 | 否 | 同渠道内换 Key，不换渠道；同渠道 Key 耗尽抛错 | 研发自动化（同供应商多 Key，K1 限流换 K2） |
| `FAIL_OVER`（失败转移） | 是 | 是 | 换 Key + 按 priority 换渠道，全耗尽抛错 | 流程自动化（跨渠道/跨供应商转移保透明） |

### 默认值与数据迁移

- 新建应用未指定时默认 `FAIL_RETRY`（契合「同供应商多 Key」主场景）
- Flyway V68 数据迁移：现有应用 `failureStrategy` 设为 `FAIL_OVER`（保持原 L0+L1 全跑行为不变）

### 与熔断器正交

- `failureStrategy` 控制候选间转移决策；`ChannelEndpointCircuitBreakerManager`（端点级熔断器）控制端点级跳过
- 端点连续失败 → OPEN → 后续请求跳过该端点；管理员可手动 forceOpen/forceClose 应急
- 策略与熔断器互不依赖，可组合使用

### 不做共因跳过

故障时不按 clusterId/providerId 跳过候选。Cluster 故障域实体与 clusterId 字段已删除。共因渠道（如 OpenAI 官方+Azure）首次故障多试一次，由端点级熔断器在连续失败后 OPEN 跳过。避免误杀不共因候选（同供应商不同账户 Key 账户额度独立、故障不共因，若归同一 clusterId 会被共因跳过误杀）。

## 四、错误分流表：L1/NONE 路由决策

`ErrorClassifier.classify(errorType)` 按 `ProviderErrorType` 映射到 `FailoverDecision`（L1/NONE），指导转移决策：

| 错误类型 | 转移决策 | 理由 |
|---------|---------|------|
| `INVALID_REQUEST` | **NONE**（不转移，直接抛） | 请求级错误换哪都无效，避免雪崩 |
| `AUTHENTICATION_ERROR` | L1（换渠道） | 共因故障：同账号换 Key/模型无效，换账号有效 |
| `RATE_LIMIT_ERROR` | L1（换渠道） | 共因故障：账号级/渠道级限流，换渠道保透明 |
| `QUOTA_EXCEEDED` | L1（换渠道） | 共因故障：配额账号级共用 |
| `TIMEOUT_ERROR` | L1（换渠道） | 共因故障：端点级超时 |
| `UPSTREAM_ERROR` | L1（换渠道） | 共因故障：上游服务异常 |
| `SERVICE_UNAVAILABLE` | L1（换渠道） | 共因故障：上游不可用 |
| `NETWORK_ERROR` | L1（换渠道） | 共因故障：域名/网络级故障 |
| `UNKNOWN_ERROR` | **NONE**（不转移直接抛） | 未分类错误不转移，降级决策还给应用 |
| `null` 输入 | NONE（直接抛） | 编程错误或未分类，不转移 |
| 未在表中显式映射的新增枚举 | **NONE**（兜底不转移直接抛） | 防御性兜底，不转移 |

硬规则：**`INVALID_REQUEST` 绝不转移**（NONE），所有场景一致，无视应用策略。共因故障（AUTH/RATE_LIMIT/QUOTA/TIMEOUT/UPSTREAM/SERVICE_UNAVAILABLE/NETWORK）返回 L1 时按应用 `failureStrategy` 控制 L0/L1 行为。`UNKNOWN_ERROR` 为 NONE（本 change 删 L2 后，未分类错误不再走模型降级，直接抛给应用）。

> `FailoverDecision` 枚举收敛为 L1/NONE（删除 L2）。分流决策由机制层 `ErrorClassifier` 统一保证正确性，不提供应用级覆盖，简化配置心智成本。

## 五、数据模型

### 5.1 应用身份载体：Application 聚合根（已实现）

Application 是权限+行为双聚合根（Key 归属 + 渠道可见性 + 应用级超时 + 应用级失败处理策略）。**本 change 新增 `failureStrategy` 字段**（承接原 ResilienceProfile 的失败处理策略语义），`timeout` 字段承接原 ResilienceProfile.timeout。

```
Application（聚合根）
  - code / name / description / state / timeout / failureStrategy / quotaBudgetId / dashboardId + 审计
```

- `applications.timeout` —— **应用级超时秒数**。0 表示用渠道默认；非 0 时由 `RoutingResolver` 注入运行时，覆盖 `channel.getTimeout()`（见第七节）。
- `applications.failure_strategy` —— **应用级失败处理策略**（`FAIL_FAST`/`FAIL_RETRY`/`FAIL_OVER`，默认 `FAIL_RETRY`）。由 `RoutingResolver` 注入 `RoutingContext`，`ChannelFailoverInvoker` 读取控制 L0/L1 行为。
- 容灾画像实体退场：原 `Application.resilienceProfileId` → `ResilienceProfile` 关联整删，timeout/failureStrategy 直接挂 Application。

**API**: `POST/PUT/GET/DELETE /api/v1/applications`（timeout 与 failureStrategy 在创建/更新请求体直接配置，无独立容灾画像绑定端点）。

### 5.2 ApplicationChannel：应用级渠道授权与转移顺序（已实现）

`ApplicationChannel` 关联实体决定应用可见的渠道集合，并承载**应用级转移顺序（priority）**。

```
ApplicationChannel
  - applicationId / channelId / priority + 审计
  - 唯一约束 (application_id, channel_id)
```

- `priority` — 应用级转移顺序，数值越小越先试。同一渠道对不同应用可有不同 priority（渠道A 对客服应用 priority=1，对内部工具 priority=3）。**完全取代原全局 `ModelInstance.priority`**（ModelInstance.priority 退场，见 follow-up）。无主备之分，只有先后次序——所有候选资格平等，区别仅在尝试顺序。为 null 时回退默认值 100。
- 转移顺序由管理员通过前端配置，跨供应商是 priority 排序的自然结果。

**API**（gap2 新增管理端保存）:
- `GET /api/v1/applications/{id}/channels` — 查询应用授权的渠道列表（每项含 `channelId` 与 `priority`）
- `PUT /api/v1/applications/{id}/channels` — 更新应用渠道授权（先清空旧关联，再批量保存新关联含 priority；HTTP 204）
  - Request Body: `{ "channels": [{ "channelId": 1, "priority": 1 }, { "channelId": 2, "priority": 2 }] }`

### 5.3 路由器链（已实现，删 ClusterAffinity/PinnedModel，Priority 改排序器）

路由器顺序为 `Permission(@100) → EndpointHealth(@200) → Priority(@300) → LoadBalance(@9999)`：先按权限过滤、再剔除熔断端点、再在存活候选里按 priority 排序输出完整列表、最后透传候选列表。

- `PermissionRouter`（@100，isForce=true）：按 `ApplicationChannel` 授权过滤应用可见渠道，再过滤活跃 Channel（`state.isRoutable()`）。`applicationId` 为 null 时返回空集。
- `HealthRouter`（@200，isForce=true）：按端点粒度过滤熔断中实例。熔断 key 统一为 `endpointId`（运行时派生：按入站协议经 `EndpointResolver` 从 `channelId` 派生 `endpointId`，与 `KeyFailoverInvoker` 共享同一 `ChannelEndpointCircuitBreakerManager`）。
- `PriorityRouter`（@300，isForce=true）：**排序器**（非选择器）——按应用级 `ApplicationChannel.priority` 升序输出完整候选列表，SHALL NOT 收敛到最优组（修复原选择器 isForce=true 导致备候选被丢、L1 换不到备的缺陷）。排序键为 `RoutingRequest.channelPriorityMap.get(channelId)`，null 回退默认值 100。
- `LoadBalanceRouter`（@9999，isForce=false）：降级为透传（候选列表产出后不再收敛到单实例），供 L1 故障转移逐个尝试。

> 删除的路由器：`ClusterAffinityRouter`（@250，域级预判与端点级 HealthRouter 等价，删除域级聚合后无意义）、`PinnedModelRouter`（@350，模型锁定由应用侧选模型实现）。

`InstanceSelector.select` 返回**按 ApplicationChannel.priority 升序排序的候选列表**（而非单实例），供 `ChannelFailoverInvoker` 逐个尝试。

## 六、应用级超时、渠道优先级与失败处理策略（已实现）

本 change 删除容灾画像实体与档位推导（ResilienceProfileApplier 退场），改为三个应用级直接配置项：

1. **应用级超时 `Application.timeout`**：0 表示用渠道默认；非 0 时由 `RoutingResolver` 注入运行时，覆盖 `channel.getTimeout()`。timeout 通过应用 CRUD 端点直接配置，无独立画像绑定端点。
2. **应用级渠道优先级 `ApplicationChannel.priority`**：管理员通过 `PUT /api/v1/applications/{id}/channels` 配置各渠道 priority，定义 L1 转移先后次序。`InstanceSelector` 查 `ApplicationChannelGateway.findByApplicationId` 构建 `channelPriorityMap` 填入 `RoutingRequest`，供 `PriorityRouter` 排序。
3. **应用级失败处理策略 `Application.failureStrategy`**：管理员通过应用编辑表单选择三策略之一，控制 `ChannelFailoverInvoker` 的 L0/L1 行为（见第三节）。由 `RoutingResolver` 注入 `RoutingContext.failureStrategy()`，`ChannelFailoverInvoker` 运行时从主候选读取。

> 原容灾画像档位（STANDARD/STRICT/AGGRESSIVE/BATCH）、L2 门禁（`enableL2ModelDegradation`/`degradationMaxDepth`）、会话亲和、模型锁定等专家字段全部退场。管理员不再面对「选容灾模式档位」，改为直接配应用 timeout、failureStrategy 与渠道 priority。容灾栈对管理员藏起来（L0/L1 是内部机制），见 `docs/容灾管理范式.md`。

## 七、运行时侵入点（已实现）

1. **认证后**（`ApiKeyAuthInterceptor` 解析 apiKeyId → applicationId）：`InstanceSelector.select` 构建应用级 `channelPriorityMap` 填入 `RoutingRequest`，贯穿 RouterChain。
2. **`RoutingRequest`** 携带 `applicationId` + `protocol` + `channelPriorityMap`（无 `resilienceProfile` 字段，画像退场）：
   - `HealthRouter`（@200）：按 endpointId 过滤熔断端点
   - `PriorityRouter`（@300）：按应用级 priority 升序排序输出完整列表
   - `LoadBalanceRouter`（@9999）：降级为透传，产出候选列表
3. **`RoutingResolver`**：从 `Application.getTimeout()` 读取应用级超时，非 0 时覆盖 `channel.getTimeout()` 作为本次请求超时；从 `Application.getFailureStrategy()` 读取策略填入 `RoutingContext.failureStrategy()`。
4. **`ChannelFailoverInvoker`**：候选内逐个试，按 `ErrorClassifier.classify(errorType)` 决定 L1/NONE，再按应用 `failureStrategy` 控制 L0/L1 行为，全耗尽抛最后异常（不再进 L2）。

```
ChannelFailoverInvoker.invoke(候选列表, request, applicationId, traceId):
  strategy = primaryCtx.failureStrategy() ?? FAIL_RETRY   // 从主候选读取，null 回退默认
  lastException = null
  for i, instance in 候选列表:                          // 已按 ApplicationChannel.priority 升序排序
    ctx = resolve(同模型, instance.channelId)
    try:
      if strategy == FAIL_FAST:
        return keyFailoverInvoker.invokeSingleKey(ctx, request)    // 只试首个 Key，不跑 L0
      else:                                                        // FAIL_RETRY / FAIL_OVER
        return keyFailoverInvoker.invoke(ctx, request)             // 跑 L0 同渠道换 Key
    catch ProviderException e:
      decision = errorClassifier.classify(e.errorType)             // L1 / NONE
      if decision == NONE: throw e                                 // 请求级错误直接抛，无视策略
      lastException = e
      if strategy == FAIL_FAST: throw e                            // FAIL_FAST 首个 Key 失败立即抛
      if strategy == FAIL_RETRY: break                             // 同渠道 Key 耗尽，不换候选不跑 L1，不发事件
      // FAIL_OVER：换候选前发转移事件，继续下一候选
      publishFailoverEvent(候选[i], 候选, applicationId, e.errorType, decision, traceId)
      continue
  throw lastException   // 候选全耗尽（FAIL_OVER 全试完 / FAIL_RETRY break），抛最后异常（不再进 L2）
```

> 与原四层伪代码的差异：删除 L2 降级回路（`tryL2Degradation`/`L2DegradationRequiredException`），L1 全耗尽直接抛最后异常。删除 clusterId 共因跳过：L1 决策成立时不再标记 clusterId 跳过同域候选。新增应用 `failureStrategy` 策略分流：`FAIL_FAST` 只试首个 Key、`FAIL_RETRY` 同渠道换 Key 不换候选、`FAIL_OVER` 换候选发转移事件。

流式边界：**只在首字节前转移**，首字节后失败不换渠道（继承现有 `KeyFailoverInvoker.invokeStream` 约束）。

恢复机制：L1 复用 `CircuitBreaker` half-open（`OPEN` 超时后自动转 `HALF_OPEN` 试探放行，成功转 `CLOSED`，失败转 `OPEN`）。端点级熔断器是故障跳过的补充机制：端点连续失败 → OPEN → 后续请求跳过；管理员可手动 forceOpen/forceClose 应急。

## 八、转移事件流（已实现）

容灾可观测性读侧：独立 `FailoverEvent` domain 记录每次候选转移，供控制台总览页轮询渲染。

### 事件发布与持久化

`ChannelFailoverInvoker` 在 `FAIL_OVER` 策略下 L1 决策换下一候选前，经既有 `DomainEventPublisher` 发布 `FailoverOccurredEvent`（DomainEvent），由 `FailoverEventListener` 持久化为 `FailoverEvent` 实体。`FAIL_FAST`/`FAIL_RETRY` 不换候选，不发转移事件。

- **监听机制**：`FailoverEventListener` 用 **`@EventListener`**（非 `@TransactionalEventListener`）。原因：调用链 `ChatDispatchServiceImpl.dispatch` 无 `@Transactional`，整个请求处理不开启事务，`@TransactionalEventListener(AFTER_COMMIT)` 在无事务上下文时静默丢弃事件。改为 `@EventListener` 后无事务上下文下事件仍被处理。
- **同步处理**：项目未配置 `@EnableAsync`，`@EventListener` 在同一线程内同步调用监听器完成持久化（发布即持久化），调用链在持久化完成后才继续。可观测性持久化开销在毫秒级，对 10k QPS 调用链影响可接受。
- **可靠性边界**：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，非计费/审计关键路径）。监听器捕获持久化异常仅记日志，不阻断业务。

### 事件字段

`FailoverEvent`：`traceId`（串联同请求多次转移）、`applicationId`、`fromChannelId`/`fromEndpointId`、`toChannelId`/`toEndpointId`（exhausted 时为 null）、`errorType`（`ProviderErrorType` 枚举名）、`decision`（`L1`/`NONE`，L2 已删）、`exhausted`（候选是否全部耗尽）、`occurredAt`。

> **删除字段**：`fromClusterId`/`toClusterId`/`commonCauseSkip`（Cluster 故障域与共因跳过已删除，Flyway V67 删除 failover_events 表对应列）。

### 查询端点（`ResilienceEventController`）

- `GET /api/v1/resilience/events` — 转移事件流查询（分页 + `since`/`applicationId` 过滤，按 `occurredAt` 倒序）。`limit` 默认 100，上限 500。
- `GET /api/v1/resilience/events/exhausted` — 耗尽告警查询（`exhausted=true` 近期事件，按 `occurredAt` 倒序）。`since` 不传时由 Service 层补默认窗口最近 1 小时，`limit` 默认 50。

> **删除参数**：`clusterId` 过滤参数（Cluster 实体与 clusterId 字段已删除）。

前端总览页 10s 轮询渲染转移事件流与耗尽告警。

## 九、控制台管理范式

控制台三屏 + 应急操作，详见 `docs/容灾管理范式.md`：

- **容灾总览页**（只读）：转移事件流（删 clusterId/共因跳过列，10s 轮询）+ 耗尽告警 + 端点熔断状态大盘（各端点 CLOSED/OPEN/HALF_OPEN + 应急操作入口）
- **应用管理页**：应用级 `failureStrategy` 策略配置 + `timeout` 配置 + 渠道授权 `priority` 配置（定义 L1 转移先后次序）
- **Channels 应急操作**：一键熔断 force-open / 恢复 force-close / 状态查询 state（紧切域功能随 Cluster 实体删除而移除）

## 十、已落地范围与待办

本 change 已落地：Cluster 故障域实体与 clusterId/commonCauseSkip 全链路删除（含 Flyway V65 删 clusters 表、V66 删 channels.cluster_id、V67 删 failover_events 三个字段）、共因跳过逻辑删除、Application.failureStrategy 字段加法（含 Flyway V68 加列 + 数据迁移现有应用设 FAIL_OVER）、ChannelFailoverInvoker 按应用策略控制 L0/L1、前端 Cluster 清除 + 策略配置 UI + 端点熔断应急 UI + 总览页重组（删 Cluster 拓扑 + 加端点熔断状态大盘）。

待办（未在本 change 实现，留作后续）：
- **`ModelInstance.priority` 字段/列物理删除（follow-up）**：本 change spec 已声明 ModelInstance.priority 退场，运行时转移顺序已完全由 `ApplicationChannel.priority` 驱动（`PriorityRouter` 用 `channelPriorityMap` 精排覆盖 DB 粗排）。但 `ModelInstance` 实体/`ModelInstanceDo`/`model_instances.priority` 列的物理删除由用户决策拆为独立后续 change，本 change 未执行物理删除。priority 字段当前仅作 DB 粗排兜底，不再决定最终转移顺序。
- 转移模式 `transferMode`（FAST/QUEUED）
- 成本策略 `costStrategy`（COST_OPTIMIZED/LATENCY_OPTIMIZED）
- 就近路由（按 region 偏好择域）——依赖请求级 region 上下文
- `traceId` 透传——OpenTelemetry 接入后填充转移事件 traceId
