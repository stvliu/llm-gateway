---
change: refactor-resilience-to-application-strategy
design-doc: docs/superpowers/specs/2026-07-02-refactor-resilience-to-application-strategy-design.md
base-ref: b90d0e88b44aecaa09c9e8291600fc0e0046f3d5
archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

# 容灾重构：删除 Cluster + 引入应用级失败处理策略 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 删除 Cluster 故障域根实体与共因跳过逻辑，引入应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），容灾走向由应用策略 + ApplicationChannel.priority + 端点级熔断器承担，并补齐管理员容灾管理前端功能。

**Architecture:** 后端 COLA Light 分层（domain/application/adapter/infrastructure）。减法：横切删除 clusterId/commonCauseSkip 字段链路（RoutingContext→Channel→FailoverEvent 全链）。加法：Application 新增 failureStrategy 单枚举字段（不独立实体），经 RoutingContext 透传至 ChannelFailoverInvoker 控制 L0（同渠道换 Key）/L1（换渠道）行为。熔断器（ChannelEndpointCircuitBreakerManager）与策略正交，控制端点级跳过。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + JPA + Flyway + H2/PostgreSQL；前端 React + Ant Design + TanStack Query + i18next + vitest。

**设计文档：** `docs/superpowers/specs/2026-07-02-refactor-resilience-to-application-strategy-design.md`
**任务边界：** `openspec/changes/refactor-resilience-to-application-strategy/tasks.md`

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## 三策略行为规约（Task 5-7 共同遵循）

| 策略 | L0 同渠道换 Key | L1 换渠道 | 行为 |
|------|----------------|----------|------|
| `FAIL_FAST`（快速失败） | 否 | 否 | 候选首个 Key 失败立即抛错（不调 KeyFailoverInvoker 换 Key、不换渠道） |
| `FAIL_RETRY`（失败重试，默认） | 是 | 否 | KeyFailoverInvoker 试完同渠道所有 Key，Key 耗尽抛错（不换渠道） |
| `FAIL_OVER`（失败转移） | 是 | 是 | 试完同渠道 Key 后按 priority 换下一渠道，全耗尽抛错（当前行为） |

三者递进：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER（L0/L1 逐级启用）。默认 FAIL_RETRY（契合同供应商多 Key 主场景）。现有应用数据迁移为 FAIL_OVER（保持原行为）。

**透传方式（设计 D6 决策）：** RoutingContext 新增 `failureStrategy` 字段；RoutingResolver 已在 `resolveApplicationTimeout` 查询 Application 一次，同时取 failureStrategy 填入每个候选的 RoutingContext，避免每请求重复查 DB。ChannelFailoverInvoker 从 `primaryCtx.failureStrategy()` 读取（所有候选同应用同策略）。

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## 文件结构映射

### 后端 — 删除文件（Cluster 根实体全套）
- `domain/resilience/entity/Cluster.java` — 故障域根实体实体
- `domain/resilience/gateway/ClusterGateway.java` — 故障域领域网关接口
- `infrastructure/resilience/gateway/ClusterGatewayImpl.java` — 网关实现
- `infrastructure/resilience/gateway/database/dataobject/ClusterDo.java` — 数据对象
- `infrastructure/resilience/gateway/database/repository/ClusterRepository.java` — JPA Repository
- `application/resilience/ClusterService.java` — 管理服务接口
- `application/resilience/ClusterServiceImpl.java` — 管理服务实现
- `application/resilience/dto/ClusterRequest.java` / `ClusterResponse.java` — DTO
- `adapter/api/ClusterController.java` — REST 控制器
- 对应测试：`ClusterControllerIT.java`、`ClusterGatewayImplTest.java`

### 后端 — 修改文件（删 clusterId/commonCauseSkip 字段）
- `domain/supply/valueobject/RoutingContext.java` — 删 clusterId 字段
- `application/proxy/routing/RoutingResolver.java` — 删 buildContext 的 clusterId 填充
- `domain/supply/entity/Channel.java` — 删 clusterId 字段
- `infrastructure/supply/gateway/database/dataobject/ChannelDo.java` — 删 clusterId 列映射
- `infrastructure/supply/gateway/ChannelGatewayImpl.java` — 删 toEntity/toDataObject 的 clusterId 转换（行 93/115）
- `application/channel/dto/ChannelResponse.java` — 删 clusterId 字段
- `application/channel/ChannelServiceImpl.java` — 删 setClusterId（行 232）
- `common/event/FailoverOccurredEvent.java` — 删 fromClusterId/toClusterId/commonCauseSkip 字段
- `domain/resilience/entity/FailoverEvent.java` — 删同上字段
- `infrastructure/resilience/gateway/database/dataobject/FailoverEventDo.java` — 删同上列映射
- `application/resilience/dto/FailoverEventResponse.java` — 删同上字段
- `infrastructure/resilience/gateway/FailoverEventGatewayImpl.java` — 删字段透传 + findRecent 的 clusterId 参数
- `infrastructure/resilience/gateway/database/repository/FailoverEventRepository.java` — 删 findRecent 的 clusterId 参数与 @Query 条件
- `domain/resilience/gateway/FailoverEventGateway.java` — 删 findRecent 的 clusterId 参数
- `application/resilience/event/FailoverEventListener.java` — 删 toEntity 的字段透传
- `application/resilience/ResilienceEventService.java` / `ResilienceEventServiceImpl.java` — 删 findRecent 的 clusterId 参数
- `adapter/api/ResilienceEventController.java` — 删 list 的 clusterId 请求参数
- `application/proxy/invoker/ChannelFailoverInvoker.java` — 删共因跳过逻辑 + publishFailoverEvent 字段

### 后端 — 新增/修改文件（应用级失败处理策略）
- 新增 `domain/application/enums/FailureStrategy.java` — 失败处理策略枚举
- `domain/application/entity/Application.java` — 加 failureStrategy 字段
- `infrastructure/application/gateway/database/dataobject/ApplicationDo.java` — 加 failure_strategy 列映射
- `application/application/dto/ApplicationRequest.java` / `ApplicationResponse.java` — 加 failureStrategy 字段
- `application/application/ApplicationServiceImpl.java` — create/update/toResponse 透传 failureStrategy（默认 FAIL_RETRY）
- `domain/supply/valueobject/RoutingContext.java` — 加 failureStrategy 字段
- `application/proxy/routing/RoutingResolver.java` — resolveApplicationTimeout 改为查 Application 同时取 failureStrategy
- `application/proxy/invoker/KeyFailoverInvoker.java` — 新增 invokeSingleKey/invokeSingleKeyStream（FAIL_FAST 用）
- `application/proxy/invoker/ChannelFailoverInvoker.java` — 按策略控制 L0/L1

### Flyway — 新增文件
- `V65__drop_clusters_table.sql`
- `V66__drop_channel_cluster_id.sql`
- `V67__drop_failover_events_cluster_columns.sql`
- `V68__add_application_failure_strategy.sql`

### 前端 — 删除文件
- `pages/resilience/overview/grouping.ts` — Cluster 分组纯函数
- `pages/resilience/overview/__tests__/grouping.test.ts`

### 前端 — 修改文件
- `types/resilience.ts` — 删 Cluster/ClusterRequest，FailoverEvent 删 fromClusterId/toClusterId/commonCauseSkip，FailoverEventQuery 删 clusterId
- `pages/resilience/api.ts` — 删 clusters CRUD（保留 circuitBreaker + events）
- `services/query/useResilience.ts` — 删 useClusters/useCluster/useCreateCluster/useUpdateCluster + clusterKeys
- `pages/resilience/overview/index.tsx` — 删 ClusterCard + 拓扑区 + 共因跳过列，新增端点熔断状态大盘
- `types/application.ts` — 加 failureStrategy 类型
- `types/channel.ts` — 删 Channel/ChannelResponse 的 clusterId 字段
- `pages/Applications/ApplicationFormModal.tsx` — 加策略选择下拉
- `pages/Channels/`（端点列表区）— 加端点熔断应急 UI
- resilience / applications / channels 命名空间 locale 文件 — 文案适配

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## 依赖关系

```
Task 1 (删 Cluster) ─┐
Task 2 (删共因跳过行为) ─┼─→ Task 3 (删字段全链路) ─→ Task 4 (grep 检查)
                      │
Task 5 (FailureStrategy+Application) ─→ Task 6 (RoutingContext 透传) ─→ Task 7 (Invoker 按策略)
                                                                        ↓
Task 8 (Flyway) ─────────────────────────────────────────────────────→ 后端回归
                                                                        ↓
Task 9 (前端 Cluster 清除) ─→ Task 10 (策略配置 UI) ─→ Task 11 (熔断应急 UI) ─→ Task 12 (总览页重组)
                                                                        ↓
                                                              Task 13 (spec/文档) ─→ Task 14 (全链路回归)
```

**关键编译约束：** Task 2 和 Task 3 必须按序——Task 2 先移除共因跳过行为（publishFailoverEvent 暂传 false 占位），Task 3 再横切删除字段定义，保证每步可编译。Task 5-7（加法）与 Task 1-3（减法）可并行分支。

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 1: 删除 Cluster 根实体全套

**Files:**
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/resilience/entity/Cluster.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/domain/resilience/gateway/ClusterGateway.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/gateway/ClusterGatewayImpl.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/gateway/database/dataobject/ClusterDo.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/resilience/gateway/database/repository/ClusterRepository.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/resilience/ClusterService.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/resilience/ClusterServiceImpl.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/application/resilience/dto/ClusterRequest.java` / `ClusterResponse.java`
- Delete: `gateway-boot/src/main/java/com/codingas/gateway/adapter/api/ClusterController.java`
- Delete: `gateway-boot/src/test/java/com/codingas/gateway/adapter/api/ClusterControllerIT.java`
- Delete: `gateway-boot/src/test/java/com/codingas/gateway/infrastructure/resilience/gateway/ClusterGatewayImplTest.java`

**说明：** Cluster 根实体与 Channel.clusterId 是物理 ID 关联（无 FK 约束），删除 Cluster 全套不影响 Channel.clusterId 字段编译（字段在 Task 3 删）。Cluster 删除后 Channel.clusterId 成为悬空物理 ID，由 Task 3 清理。

- [x] **Step 1: 删除 Cluster 全套源文件与测试**

```bash
cd gateway-boot/src/main/java/com/codingas/gateway
rm domain/resilience/entity/Cluster.java \
   domain/resilience/gateway/ClusterGateway.java \
   infrastructure/resilience/gateway/ClusterGatewayImpl.java \
   infrastructure/resilience/gateway/database/dataobject/ClusterDo.java \
   infrastructure/resilience/gateway/database/repository/ClusterRepository.java \
   application/resilience/ClusterService.java \
   application/resilience/ClusterServiceImpl.java \
   application/resilience/dto/ClusterRequest.java \
   application/resilience/dto/ClusterResponse.java \
   adapter/api/ClusterController.java
```

```bash
cd gateway-boot/src/test/java/com/codingas/gateway
rm adapter/api/ClusterControllerIT.java \
   infrastructure/resilience/gateway/ClusterGatewayImplTest.java
```

- [x] **Step 2: 编译验证（预期无 Cluster 引用残留报错）**

Run: `./mvnw -pl gateway-boot -am compile`
Expected: BUILD SUCCESS。若报错指向残留 import，移除对应 import 行。

- [x] **Step 3: 提交**

```bash
git add -A gateway-boot
git commit -m "refactor(resilience): 删除 Cluster 故障域根实体全套

删除 Cluster 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service 及测试。
Cluster 与 Application 职责交叉且共因跳过误杀不共因候选，整体退场。
Channel.clusterId 字段由后续任务清理。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 2: 删除 ChannelFailoverInvoker 共因跳过行为

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/ChannelFailoverInvoker.java`

**说明：** 本任务只移除"共因跳过行为"——删 commonCauseFailedClusters Set、跳过判定、clusterId 标记。publishFailoverEvent 的 commonCauseSkip 参数暂传 false（字段在 Task 3 删）。行为变更：不再共因跳过，所有候选按顺序试。

- [x] **Step 1: 重写 invoke 方法（非流式）**

替换 `invoke` 方法体（当前行 104-159）为：

```java
public ProtocolResponse invoke(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                                String traceId) {
    ProviderException lastException = null;

    for (int i = 0; i < candidates.size(); i++) {
        RoutingContext candidate = candidates.get(i);
        try {
            ProtocolRequest candidateReq = adaptRequestForCandidate(request, candidate);
            ProtocolResponse response = keyFailoverInvoker.invoke(candidate, candidateReq);
            return adaptResponseForCandidate(response, candidate);
        } catch (ProviderException e) {
            FailoverDecision decision = errorClassifier.classify(e.getErrorType());
            log.warn("候选渠道 channelId={} endpointId={} 失败: {} (决策:{}), 尝试下一候选",
                    candidate.channelId(), candidate.channelEndpointId(),
                    e.getErrorType(), decision);

            if (decision == FailoverDecision.NONE) {
                throw e;
            }
            publishFailoverEvent(candidate, candidates, i, applicationId,
                    e.getErrorType(), decision, traceId);
            lastException = e;
        }
    }

    if (lastException != null) {
        throw lastException;
    }
    throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
            "候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
}
```

- [x] **Step 2: 重写 invokeStream 方法（流式）**

替换 `invokeStream` 方法体（当前行 186-273），删除 commonCauseFailedClusters Set、共因跳过 if 块、catch 中 clusterId 标记。保留首字节追踪与换候选逻辑：catch 中 `firstByteSent` 为 true 直接抛；否则 NONE 直接抛，其余 publishFailoverEvent + 记录 lastException。

- [x] **Step 3: publishFailoverEvent 调用统一传 false**

将 invoke/invokeStream 中 `publishFailoverEvent` 调用的最后一个参数（原 `true`/`false`）统一改为 `false`。方法签名暂保留 `boolean commonCauseSkip` 参数（Task 3 删）。

- [x] **Step 4: 移除 import HashSet/Set**

`ChannelFailoverInvoker.java` 顶部删除 `import java.util.HashSet;` 和 `import java.util.Set;`。

- [x] **Step 5: 适配共因跳过相关测试**

`ChannelFailoverIntegrationTest` 中 `e2e_commonCauseFailure_skipsSameCluster_*` 等测试改为断言"不跳过、按顺序试所有候选"。删除共因跳过断言，改为验证全候选耗尽后抛错。

- [x] **Step 6: 编译并运行测试**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChannelFailoverIntegrationTest`
Expected: BUILD SUCCESS，测试通过。

- [x] **Step 7: 提交**

```bash
git add gateway-boot
git commit -m "refactor(resilience): 移除 ChannelFailoverInvoker 共因跳过行为

删除 commonCauseFailedClusters 局部 Set 与同域跳过判定，所有候选按
priority 顺序逐个尝试。publishFailoverEvent 的 commonCauseSkip 暂传
false，字段清理由后续任务完成。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 3: 删除 clusterId/commonCauseSkip 字段全链路

**Files:**
- Modify: 18 个文件（见文件结构映射"修改文件"清单）

**说明：** 横切删除 clusterId/commonCauseSkip 字段定义与所有转换/查询参数。机械删除，一次性编译通过。

- [x] **Step 1: RoutingContext 删 clusterId 字段**

`domain/supply/valueobject/RoutingContext.java`：record 定义删除 `Long clusterId` 末行，Javadoc 删 `@param clusterId`。

- [x] **Step 2: RoutingResolver 删 clusterId 填充**

`application/proxy/routing/RoutingResolver.java` 行 145-156：`buildContext` 的 RoutingContext 构造删除 `channel.getClusterId()` 实参。

- [x] **Step 3: Channel / ChannelDo 删 clusterId**

- `domain/supply/entity/Channel.java` 行 51-52：删 `private Long clusterId;` 及注释
- `infrastructure/supply/gateway/database/dataobject/ChannelDo.java` 行 57-59：删 `clusterId` 字段及 `@Column(name = "cluster_id")`

- [x] **Step 4: ChannelGatewayImpl 删 clusterId 转换**

`infrastructure/supply/gateway/ChannelGatewayImpl.java` 行 93 删 `entity.setClusterId(doObj.getClusterId());`，行 115 删 `doObj.setClusterId(entity.getClusterId());`。

- [x] **Step 5: ChannelResponse / ChannelServiceImpl 删 clusterId**

- `application/channel/dto/ChannelResponse.java` 行 39：删 `private Long clusterId;`
- `application/channel/ChannelServiceImpl.java` 行 232：删 `response.setClusterId(channel.getClusterId());`

- [x] **Step 6: FailoverOccurredEvent 删 3 字段**

`common/event/FailoverOccurredEvent.java`：record 删除 `fromClusterId`/`toClusterId`/`commonCauseSkip` 三个字段，删除 12 参数次级构造器（不再需要兼容）。规范构造器变为 10 参数。更新 Javadoc。

- [x] **Step 7: FailoverEvent 实体删 3 字段**

`domain/resilience/entity/FailoverEvent.java`：删 `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段及注释。

- [x] **Step 8: FailoverEventDo 删 3 列映射**

`infrastructure/resilience/gateway/database/dataobject/FailoverEventDo.java`：删 `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段及 `@Column`。

- [x] **Step 9: FailoverEventResponse 删 3 字段**

`application/resilience/dto/FailoverEventResponse.java`：删 `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段。

- [x] **Step 10: FailoverEventGatewayImpl 删字段透传 + findRecent 参数**

`infrastructure/resilience/gateway/FailoverEventGatewayImpl.java`：
- `toEntity`/`toDataObject` 删除 fromClusterId/toClusterId/commonCauseSkip 透传
- `findRecent` 签名删 `Long clusterId` 参数，调用 repository 删该实参

- [x] **Step 11: FailoverEventRepository 删 clusterId 参数**

`infrastructure/resilience/gateway/database/repository/FailoverEventRepository.java`：`findRecent` 删 `@Param("clusterId") Long clusterId`，@Query 删除 clusterId 过滤子句。

- [x] **Step 12: FailoverEventGateway 接口删 clusterId 参数**

`domain/resilience/gateway/FailoverEventGateway.java`：`findRecent` 删 `Long clusterId` 参数，更新 Javadoc。

- [x] **Step 13: FailoverEventListener 删字段透传**

`application/resilience/event/FailoverEventListener.java`：`toEntity` 删除 `setFromClusterId`/`setToClusterId`/`setCommonCauseSkip` 三行。

- [x] **Step 14: ResilienceEventService/Impl 删 clusterId 参数**

- `application/resilience/ResilienceEventService.java`：`findRecent` 删 `Long clusterId` 参数
- `application/resilience/ResilienceEventServiceImpl.java`：`findRecent` 删 `Long clusterId` 参数及透传

- [x] **Step 15: ResilienceEventController 删 clusterId 请求参数**

`adapter/api/ResilienceEventController.java`：`list` 方法删 `@RequestParam(required = false) Long clusterId` 及调用透传。

- [x] **Step 16: ChannelFailoverInvoker.publishFailoverEvent 删字段**

`application/proxy/invoker/ChannelFailoverInvoker.java`：
- `publishFailoverEvent` 签名删 `boolean commonCauseSkip` 参数
- 方法体删除 `fromClusterId`/`toClusterId` 局部变量
- `FailoverOccurredEvent` 构造删 3 个实参，更新为 10 参数构造

- [x] **Step 17: 适配既有测试**

检索测试中引用 `clusterId`/`commonCauseSkip`/`fromClusterId`/`toClusterId` 的断言与构造，删除或适配。涉及：`FailoverEventListenerTest`、`FailoverEventListenerPublishTest`、`FailoverEventRepositoryTest`、`FailoverEventGatewayImplTest`、`ResilienceEventServiceImplTest`、`ResilienceEventControllerIT`、`ChatDispatchServiceTest`、`RoutingResolverTest`、`FullContextIntegrationTest` 等。

- [x] **Step 18: 编译并运行全量测试**

Run: `./mvnw -pl gateway-boot -am test`
Expected: BUILD SUCCESS，所有测试通过。

- [x] **Step 19: 提交**

```bash
git add gateway-boot
git commit -m "refactor(resilience): 删除 clusterId/commonCauseSkip 字段全链路

横切清理 RoutingContext/Channel/ChannelDo/ChannelResponse/FailoverEvent
及转移事件查询 API 的 clusterId/commonCauseSkip 字段。共因跳过机制
彻底退场，L1 转移改为按 ApplicationChannel.priority 顺序逐个尝试。

BREAKING: 转移事件查询响应不再返回 fromClusterId/toClusterId/
commonCauseSkip；/resilience/events 不再接受 clusterId 查询参数。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 4: grep 残留检查

**Files:** 全仓扫描

- [x] **Step 1: 后端残留检查**

```bash
grep -rn "Cluster\|clusterId\|commonCauseSkip\|ClusterHealthAggregator\|ClusterAffinityRouter" \
  gateway-boot/src/main/java --include="*.java"
```
Expected: 无输出（或仅注释中残留，需清理）。重点关注 `Cluster` 作为类型名、`clusterId` 作为字段/参数名、`commonCauseSkip` 作为字段/参数名。

- [x] **Step 2: 测试残留检查**

```bash
grep -rn "clusterId\|commonCauseSkip\|fromClusterId\|toClusterId" \
  gateway-boot/src/test/java --include="*.java"
```
Expected: 无输出。

- [x] **Step 3: 清理残留**

对 grep 命中的残留逐个清理（删除 import、字段引用、注释中的过期描述）。

- [x] **Step 4: 提交（若有清理）**

```bash
git add -A
git commit -m "chore(resilience): 清理 Cluster/clusterId/commonCauseSkip 残留引用

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 5: FailureStrategy 枚举 + Application 链路（TDD）

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/application/enums/FailureStrategy.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/application/entity/Application.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/infrastructure/application/gateway/database/dataobject/ApplicationDo.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/application/dto/ApplicationRequest.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/application/dto/ApplicationResponse.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/application/ApplicationServiceImpl.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/application/ApplicationServiceImplTest.java`

- [x] **Step 1: 创建 FailureStrategy 枚举**

```java
package com.codingas.gateway.domain.application.enums;

/**
 * 应用级失败处理策略（三选一互斥）
 *
 * <p>控制 ChannelFailoverInvoker 的 L0（同渠道换 Key）/L1（换渠道）行为：
 * <ul>
 *   <li>FAIL_FAST — L0 不跑、L1 不跑，首个 Key 失败立即抛错</li>
 *   <li>FAIL_RETRY — L0 跑、L1 不跑，同渠道换 Key 不换渠道（默认）</li>
 *   <li>FAIL_OVER — L0 跑、L1 跑，换 Key + 换渠道，全耗尽抛错</li>
 * </ul>
 * 递进关系：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER。
 *
 * <p>轻量单字段挂 Application，不演变为已删的 ResilienceProfile（独立实体）。</p>
 */
public enum FailureStrategy {
    /** 快速失败：首个 Key 失败立即抛错 */
    FAIL_FAST,
    /** 失败重试：同渠道换 Key，不换渠道（默认） */
    FAIL_RETRY,
    /** 失败转移：换 Key + 换渠道，全耗尽抛错 */
    FAIL_OVER
}
```

- [x] **Step 2: Application 实体加 failureStrategy 字段**

`domain/application/entity/Application.java`：在 `timeout` 字段后新增：

```java
    /** 应用级失败处理策略（默认 FAIL_RETRY） */
    private FailureStrategy failureStrategy;
```

全参构造器新增 `FailureStrategy failureStrategy` 参数并赋值。加 import `com.codingas.gateway.domain.application.enums.FailureStrategy`。

- [x] **Step 3: ApplicationDo 加 failure_strategy 列**

`infrastructure/application/gateway/database/dataobject/ApplicationDo.java`：在 `timeout` 字段后新增：

```java
    /** 应用级失败处理策略（枚举名存储：FAIL_FAST/FAIL_RETRY/FAIL_OVER） */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_strategy", nullable = false, length = 16)
    private com.codingas.gateway.domain.application.enums.FailureStrategy failureStrategy;
```

加 import `jakarta.persistence.EnumType` 和 `jakarta.persistence.Enumerated`（若未有）。

- [x] **Step 4: ApplicationGatewayImpl 适配 failureStrategy 透传**

`infrastructure/application/gateway/ApplicationGatewayImpl.java`：`toEntity`/`toDataObject` 新增 `failureStrategy` 字段互转（`entity.setFailureStrategy(doObj.getFailureStrategy())` 与 `doObj.setFailureStrategy(entity.getFailureStrategy())`）。

- [x] **Step 5: ApplicationRequest/ApplicationResponse 加字段**

- `ApplicationRequest.java` 加：
```java
    /** 应用级失败处理策略（不传时后端默认 FAIL_RETRY） */
    private com.codingas.gateway.domain.application.enums.FailureStrategy failureStrategy;
```
- `ApplicationResponse.java` 加：
```java
    /** 应用级失败处理策略（FAIL_FAST/FAIL_RETRY/FAIL_OVER） */
    private String failureStrategy;
```

- [x] **Step 6: ApplicationServiceImpl 透传 failureStrategy**

`application/application/ApplicationServiceImpl.java`：
- `create`：`app.setFailureStrategy(request.getFailureStrategy() != null ? request.getFailureStrategy() : FailureStrategy.FAIL_RETRY);`
- `update`：同上
- `toResponse`：`response.setFailureStrategy(app.getFailureStrategy() != null ? app.getFailureStrategy().name() : null);`

加 import `com.codingas.gateway.domain.application.enums.FailureStrategy`。

- [x] **Step 7: 写测试 — 默认 FAIL_RETRY**

`ApplicationServiceImplTest.java` 新增测试：

```java
@Test
void create_withoutFailureStrategy_defaultsToFailRetry() {
    ApplicationRequest req = new ApplicationRequest();
    req.setCode("APP-TEST");
    req.setName("测试应用");
    req.setTimeout(0);
    // 不设置 failureStrategy

    ApplicationResponse resp = service.create(req);

    assertThat(resp.getFailureStrategy()).isEqualTo("FAIL_RETRY");
    verify(applicationGateway).save(argThat(a -> a.getFailureStrategy() == FailureStrategy.FAIL_RETRY));
}
```

- [x] **Step 8: 写测试 — 透传指定策略**

```java
@Test
void create_withFailFast_propagatesStrategy() {
    ApplicationRequest req = new ApplicationRequest();
    req.setCode("APP-FF");
    req.setName("快速失败应用");
    req.setFailureStrategy(FailureStrategy.FAIL_FAST);

    ApplicationResponse resp = service.create(req);

    assertThat(resp.getFailureStrategy()).isEqualTo("FAIL_FAST");
}
```

- [x] **Step 9: 运行测试**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ApplicationServiceImplTest`
Expected: PASS。

- [x] **Step 10: 提交**

```bash
git add gateway-boot
git commit -m "feat(application): 引入应用级失败处理策略 failureStrategy

Application 新增 FailureStrategy 枚举字段（FAIL_FAST/FAIL_RETRY/
FAIL_OVER），默认 FAIL_RETRY。ApplicationRequest/Response/ServiceImpl
透传。轻量单字段，不演变为 ResilienceProfile。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 6: RoutingContext + RoutingResolver 透传 failureStrategy

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/domain/supply/valueobject/RoutingContext.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/routing/RoutingResolver.java`

**说明：** RoutingContext 新增 failureStrategy 字段。RoutingResolver 已在 resolveApplicationTimeout 查 Application，改为同时取 failureStrategy 填入候选，避免每请求重复查 DB。

- [x] **Step 1: RoutingContext 加 failureStrategy 字段**

`domain/supply/valueobject/RoutingContext.java`：record 末尾新增 `FailureStrategy failureStrategy`，加 import 与 `@param` Javadoc。

```java
public record RoutingContext(
        Long channelId,
        Long channelEndpointId,
        String endpointUrl,
        Protocol upstreamProtocol,
        String providerApiKey,
        Integer timeout,
        boolean needsProtocolAdaptation,
        String modelName,
        String upstreamModelName,
        com.codingas.gateway.domain.application.enums.FailureStrategy failureStrategy
) {}
```

- [x] **Step 2: RoutingResolver 透传 failureStrategy**

`application/proxy/routing/RoutingResolver.java`：
- `resolveCandidates` 中将 `Integer applicationTimeout = resolveApplicationTimeout(applicationId);` 改为 `Application app = resolveApplication(applicationId);`，传 `app` 给 `buildContext`
- `resolveApplicationTimeout` 重构为 `resolveApplication`：

```java
private Application resolveApplication(Long applicationId) {
    if (applicationId == null) {
        return null;
    }
    Application app = applicationGateway.findById(applicationId);
    if (app == null) {
        log.warn("Application 未找到，超时与策略回退默认。applicationId={}", applicationId);
        return null;
    }
    return app;
}
```

- `buildContext` 签名改为接收 `Application app`：

```java
private RoutingContext buildContext(ModelInstance instance, Model model, Protocol protocol, Application app) {
    String apiKey = credentialResolver.resolve(instance.getChannelId());
    ChannelEndpoint endpoint = endpointResolver.resolve(instance.getChannelId(), protocol);
    Channel channel = channelGateway.findById(instance.getChannelId())
            .orElseThrow(() -> new ResourceNotFoundException("Channel", instance.getChannelId()));

    Integer effectiveTimeout = (app != null && app.getTimeout() != 0)
            ? app.getTimeout() : channel.getTimeout();
    com.codingas.gateway.domain.application.enums.FailureStrategy strategy =
            app != null && app.getFailureStrategy() != null
                    ? app.getFailureStrategy()
                    : com.codingas.gateway.domain.application.enums.FailureStrategy.FAIL_RETRY;

    boolean needsAdaptation = endpoint.getProtocol() != protocol;

    return new RoutingContext(
            channel.getId(),
            endpoint.getId(),
            endpoint.getEndpointUrl(),
            endpoint.getProtocol(),
            apiKey,
            effectiveTimeout,
            needsAdaptation,
            model.getModelName(),
            instance.getUpstreamModelName(),
            strategy
    );
}
```

- [x] **Step 3: 适配 RoutingResolverTest**

更新 `RoutingResolverTest` 中 RoutingContext 断言（新增 failureStrategy 字段断言），mock Application 返回 failureStrategy。

- [x] **Step 4: 编译并运行测试**

Run: `./mvnw -pl gateway-boot -am test -Dtest=RoutingResolverTest`
Expected: PASS。

- [x] **Step 5: 提交**

```bash
git add gateway-boot
git commit -m "feat(routing): RoutingContext 透传应用 failureStrategy

RoutingResolver.resolveApplication 查询 Application 同时取 timeout 与
failureStrategy，填入每个候选 RoutingContext，避免每请求重复查 DB。
ChannelFailoverInvoker 可从候选直取策略控制 L0/L1。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 7: ChannelFailoverInvoker 按策略控制 L0/L1（TDD）

**Files:**
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/KeyFailoverInvoker.java`
- Modify: `gateway-boot/src/main/java/com/codingas/gateway/application/proxy/invoker/ChannelFailoverInvoker.java`
- Test: `gateway-boot/src/test/java/com/codingas/gateway/application/proxy/invoker/ChannelFailoverStrategyTest.java`（新建）

**说明：** KeyFailoverInvoker 新增 invokeSingleKey/invokeSingleKeyStream（只试第一个 Key，FAIL_FAST 用）。ChannelFailoverInvoker 按策略分流：FAIL_FAST 调 invokeSingleKey 失败即抛；FAIL_RETRY 调 invoke 后 break（不换渠道）；FAIL_OVER 调 invoke 后 continue（换渠道）。

- [x] **Step 1: 写 FAIL_FAST 测试（先红）**

```java
package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.domain.application.enums.FailureStrategy;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
// ... 其他 import
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChannelFailoverStrategyTest {

    @Test
    void failFast_firstKeyFailure_throwsImmediately_noChannelSwitch() {
        // 两个候选，第一个 Key 失败应立即抛错，不试第二个候选
        KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
        ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMITED, "限流", null, "m", "openai", 1L, null);
        when(keyInvoker.invokeSingleKey(any(), any())).thenThrow(failure);

        ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
        RoutingContext c1 = ctx(1L, FailureStrategy.FAIL_FAST);
        RoutingContext c2 = ctx(2L, FailureStrategy.FAIL_FAST);

        assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(), Protocol.OPENAI, 1L, "t"))
                .isSameAs(failure);
        // 验证未换渠道：c2 的 invokeSingleKey 从未被调用
        verify(keyInvoker, never()).invokeSingleKey(eq(c2), any());
        // 验证未调 invoke（换 Key）：只调 invokeSingleKey
        verify(keyInvoker, never()).invoke(any(), any());
    }
}
```

- [x] **Step 2: 写 FAIL_RETRY 测试（先红）**

```java
@Test
void failRetry_sameChannelKeyExhausted_noChannelSwitch_throws() {
    KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
    ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMITED, "Key 耗尽", null, "m", "openai", 1L, null);
    when(keyInvoker.invoke(any(), any())).thenThrow(failure);

    ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
    RoutingContext c1 = ctx(1L, FailureStrategy.FAIL_RETRY);
    RoutingContext c2 = ctx(2L, FailureStrategy.FAIL_RETRY);

    assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(), Protocol.OPENAI, 1L, "t"))
            .isSameAs(failure);
    // 验证未换渠道：c2 从未被调用
    verify(keyInvoker, never()).invoke(eq(c2), any());
}
```

- [x] **Step 3: 写 FAIL_OVER 测试（先红）**

```java
@Test
void failOver_channelExhausted_switchesToNextCandidate() {
    KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
    ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMITED, "限流", null, "m", "openai", 1L, null);
    when(keyInvoker.invoke(eq(c1Ctx), any())).thenThrow(failure);
    when(keyInvoker.invoke(eq(c2Ctx), any())).thenReturn(successResponse());

    ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
    RoutingContext c1 = ctx(1L, FailureStrategy.FAIL_OVER);
    RoutingContext c2 = ctx(2L, FailureStrategy.FAIL_OVER);

    ProtocolResponse resp = invoker.invoke(c1, List.of(c1, c2), req(), Protocol.OPENAI, 1L, "t");
    assertThat(resp).isNotNull();
    verify(keyInvoker).invoke(eq(c2), any()); // 换到了第二个候选
}
```

- [x] **Step 4: 写默认策略测试（先红）**

```java
@Test
void defaultStrategy_whenNull_failRetry() {
    // failureStrategy 为 null 时回退 FAIL_RETRY
    KeyFailoverInvoker keyInvoker = mock(KeyFailoverInvoker.class);
    ProviderException failure = new ProviderException(ProviderErrorType.RATE_LIMITED, "限流", null, "m", "openai", 1L, null);
    when(keyInvoker.invoke(any(), any())).thenThrow(failure);

    ChannelFailoverInvoker invoker = newRealInvoker(keyInvoker);
    RoutingContext c1 = ctxWithNullStrategy(1L);
    RoutingContext c2 = ctxWithNullStrategy(2L);

    assertThatThrownBy(() -> invoker.invoke(c1, List.of(c1, c2), req(), Protocol.OPENAI, 1L, "t"))
            .isSameAs(failure);
    verify(keyInvoker, never()).invoke(eq(c2), any()); // 未换渠道
}
```

- [x] **Step 5: 运行测试确认失败（红）**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChannelFailoverStrategyTest`
Expected: FAIL（invokeSingleKey 方法不存在、策略未生效）。

- [x] **Step 6: KeyFailoverInvoker 新增 invokeSingleKey/invokeSingleKeyStream**

```java
/**
 * 单 Key 调用（FAIL_FAST 策略用）— 只试第一个可用 Key，失败即抛，不换 Key
 */
public ProtocolResponse invokeSingleKey(RoutingContext ctx, ProtocolRequest request) {
    List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
    String provider = ctx.upstreamProtocol().name().toLowerCase();
    if (credentials.isEmpty()) {
        throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                "无可用 Key", null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }
    ChannelCredential cred = credentials.get(0);
    UpstreamClient client = buildClient(ctx, cred);
    try {
        return client.chat(request);
    } catch (ProviderException e) {
        meterRegistry.counter("gateway.failover.triggered",
                "provider", provider,
                "from_key", String.valueOf(cred.getId()),
                "error_type", e.getErrorType().name()).increment();
        throw e;
    }
}

/**
 * 单 Key 流式调用（FAIL_FAST 策略用）
 */
public void invokeSingleKeyStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback) {
    List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
    String provider = ctx.upstreamProtocol().name().toLowerCase();
    if (credentials.isEmpty()) {
        throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                "流式调用：无可用 Key", null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }
    ChannelCredential cred = credentials.get(0);
    UpstreamClient client = buildClient(ctx, cred);
    try {
        client.chatStream(request, callback);
    } catch (ProviderException e) {
        meterRegistry.counter("gateway.failover.triggered",
                "provider", provider,
                "from_key", String.valueOf(cred.getId()),
                "error_type", e.getErrorType().name()).increment();
        throw e;
    }
}
```

- [x] **Step 7: ChannelFailoverInvoker.invoke 按策略分流**

替换 `invoke` 方法体：

```java
public ProtocolResponse invoke(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                                String traceId) {
    FailureStrategy strategy = primaryCtx.failureStrategy() != null
            ? primaryCtx.failureStrategy() : FailureStrategy.FAIL_RETRY;
    ProviderException lastException = null;

    for (int i = 0; i < candidates.size(); i++) {
        RoutingContext candidate = candidates.get(i);
        try {
            ProtocolRequest candidateReq = adaptRequestForCandidate(request, candidate);
            // FAIL_FAST 只试首个 Key；FAIL_RETRY/FAIL_OVER 试完同渠道所有 Key
            ProtocolResponse response = strategy == FailureStrategy.FAIL_FAST
                    ? keyFailoverInvoker.invokeSingleKey(candidate, candidateReq)
                    : keyFailoverInvoker.invoke(candidate, candidateReq);
            return adaptResponseForCandidate(response, candidate);
        } catch (ProviderException e) {
            FailoverDecision decision = errorClassifier.classify(e.getErrorType());
            log.warn("候选渠道 channelId={} endpointId={} 失败: {} (决策:{}, 策略:{})",
                    candidate.channelId(), candidate.channelEndpointId(),
                    e.getErrorType(), decision, strategy);

            if (decision == FailoverDecision.NONE) {
                throw e;
            }
            // FAIL_FAST：首个 Key 失败立即抛错，不换 Key 不换渠道
            if (strategy == FailureStrategy.FAIL_FAST) {
                throw e;
            }
            publishFailoverEvent(candidate, candidates, i, applicationId,
                    e.getErrorType(), decision, traceId);
            lastException = e;
            // FAIL_RETRY：不换渠道，同渠道 Key 耗尽抛错（跳出循环）
            if (strategy == FailureStrategy.FAIL_RETRY) {
                break;
            }
            // FAIL_OVER：继续换下一渠道（循环 continue）
        }
    }

    if (lastException != null) {
        throw lastException;
    }
    throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
            "候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
}
```

- [x] **Step 8: ChannelFailoverInvoker.invokeStream 按策略分流**

同 invoke 模式：FAIL_FAST 调 invokeSingleKeyStream 失败即抛；FAIL_RETRY 调 invokeStream 后 break；FAIL_OVER 调 invokeStream 后 continue。保留首字节追踪逻辑。

- [x] **Step 9: 运行测试确认通过（绿）**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChannelFailoverStrategyTest`
Expected: PASS。

- [x] **Step 10: 运行全量回归**

Run: `./mvnw -pl gateway-boot -am test`
Expected: BUILD SUCCESS。

- [x] **Step 11: 提交**

```bash
git add gateway-boot
git commit -m "feat(resilience): ChannelFailoverInvoker 按应用 failureStrategy 控制 L0/L1

FAIL_FAST 调 KeyFailoverInvoker.invokeSingleKey 首个 Key 失败即抛；
FAIL_RETRY 试完同渠道 Key 后 break 不换渠道；
FAIL_OVER 试完同渠道 Key 后 continue 换下一渠道。
KeyFailoverInvoker 新增 invokeSingleKey/invokeSingleKeyStream。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 8: Flyway V65-V68 迁移

**Files:**
- Create: `gateway-boot/src/main/resources/db/migration/V65__drop_clusters_table.sql`
- Create: `gateway-boot/src/main/resources/db/migration/V66__drop_channel_cluster_id.sql`
- Create: `gateway-boot/src/main/resources/db/migration/V67__drop_failover_events_cluster_columns.sql`
- Create: `gateway-boot/src/main/resources/db/migration/V68__add_application_failure_strategy.sql`

**说明：** H2 与 PostgreSQL 均支持 `DROP TABLE IF EXISTS` / `DROP COLUMN IF EXISTS`。V68 先加列默认 FAIL_RETRY，再 UPDATE 现有应用为 FAIL_OVER（保持原 L0+L1 行为）。

- [x] **Step 1: V65 — 删 clusters 表**

```sql
-- V65: 删除 Cluster 故障域表（Cluster 根实体退场）
DROP TABLE IF EXISTS clusters;
```

- [x] **Step 2: V66 — 删 channels.cluster_id 列**

```sql
-- V66: 删除 channels.cluster_id 列（共因跳过机制退场）
ALTER TABLE channels DROP COLUMN IF EXISTS cluster_id;
```

- [x] **Step 3: V67 — 删 failover_events 冗余列**

```sql
-- V67: 删除 failover_events 的 from_cluster_id/to_cluster_id/common_cause_skip 列
ALTER TABLE failover_events DROP COLUMN IF EXISTS from_cluster_id;
ALTER TABLE failover_events DROP COLUMN IF EXISTS to_cluster_id;
ALTER TABLE failover_events DROP COLUMN IF EXISTS common_cause_skip;
```

- [x] **Step 4: V68 — applications 加 failure_strategy 列 + 数据迁移**

```sql
-- V68: applications 新增 failure_strategy 列（默认 FAIL_RETRY）
ALTER TABLE applications ADD COLUMN IF NOT EXISTS failure_strategy VARCHAR(16) NOT NULL DEFAULT 'FAIL_RETRY';

-- 数据迁移：现有应用设为 FAIL_OVER（保持原 L0+L1 行为不变）
UPDATE applications SET failure_strategy = 'FAIL_OVER';
```

- [x] **Step 5: 启动应用验证迁移（H2 测试 profile）**

Run: `./mvnw -pl gateway-boot spring-boot:run`（或运行集成测试）
Expected: 应用正常启动，Flyway 迁移无报错。

- [x] **Step 6: 提交**

```bash
git add gateway-boot/src/main/resources/db/migration
git commit -m "feat(db): Flyway V65-V68 容灾重构迁移

V65 删 clusters 表；V66 删 channels.cluster_id；V67 删 failover_events
冗余列；V68 applications 加 failure_strategy 列（默认 FAIL_RETRY），
现有应用迁移为 FAIL_OVER 保持原行为。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 9: 前端 Cluster 相关清除 + types 瘦身

**Files:**
- Delete: `gateway-console/src/pages/resilience/overview/grouping.ts`
- Delete: `gateway-console/src/pages/resilience/overview/__tests__/grouping.test.ts`
- Modify: `gateway-console/src/types/resilience.ts`
- Modify: `gateway-console/src/types/channel.ts`
- Modify: `gateway-console/src/pages/resilience/api.ts`
- Modify: `gateway-console/src/services/query/useResilience.ts`

- [x] **Step 1: types/resilience.ts 删 Cluster/ClusterRequest，FailoverEvent 瘦身**

- 删除 `Cluster` 和 `ClusterRequest` interface（整段）
- `FailoverEvent` interface 删除 `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段
- `FailoverEventQuery` 删除 `clusterId` 字段
- 更新文件头注释，移除 Cluster 相关说明

- [x] **Step 2: types/channel.ts 删 clusterId**

- `Channel` interface 删 `clusterId?: number | null;`
- `ChannelResponse` interface 删 `clusterId?: number | null;`

- [x] **Step 3: pages/resilience/api.ts 删 clusters CRUD**

删除 `clusters: { list/getById/create/update }` 整段。移除 `Cluster`/`ClusterRequest` 的 import。保留 `circuitBreaker` 和 `events`。

- [x] **Step 4: services/query/useResilience.ts 删 Cluster hooks**

- 删除 `clusterKeys` 和 `useClusters`/`useCluster`/`useCreateCluster`/`useUpdateCluster`
- 移除 `ClusterRequest` 的 import
- 保留 `failoverEventKeys`/`useFailoverEvents`/`useExhaustedEvents`/熔断 hooks

- [x] **Step 5: 删除 grouping.ts 及测试**

```bash
rm gateway-console/src/pages/resilience/overview/grouping.ts
rm gateway-console/src/pages/resilience/overview/__tests__/grouping.test.ts
```

- [x] **Step 6: 类型检查与构建**

Run: `cd gateway-console && npx tsc --noEmit`
Expected: 无类型错误。若有残留引用，逐个清理。

- [x] **Step 7: 提交**

```bash
git add gateway-console
git commit -m "refactor(console): 清除前端 Cluster 相关代码与类型

删除 Cluster/ClusterRequest 类型、clusters CRUD API、useClusters 等
hooks、grouping.ts 分组函数。FailoverEvent 类型瘦身（删 clusterId/
commonCauseSkip 字段）。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 10: 应用失败处理策略配置 UI

**Files:**
- Modify: `gateway-console/src/types/application.ts`
- Modify: `gateway-console/src/pages/Applications/ApplicationFormModal.tsx`
- Modify: `gateway-console/src/locales/{zh,en}/applications.json`（定位实际 locale 路径）

- [x] **Step 1: types/application.ts 加 failureStrategy 类型**

```typescript
/** 应用级失败处理策略（与后端 FailureStrategy 枚举一致） */
export type FailureStrategy = 'FAIL_FAST' | 'FAIL_RETRY' | 'FAIL_OVER';
```

`Application` interface 加 `failureStrategy: string;`，`CreateApplicationRequest` 加 `failureStrategy?: FailureStrategy;`。

- [x] **Step 2: ApplicationFormModal 加策略选择下拉**

在 `timeout` Form.Item 后新增：

```tsx
<Form.Item
  name="failureStrategy"
  label={t('application.failureStrategy')}
  tooltip={t('application.failureStrategyHelp')}
>
  <Select placeholder={t('application.failureStrategyPlaceholder')}>
    <Select.Option value="FAIL_FAST">
      {t('application.failureStrategyFailFast')}
    </Select.Option>
    <Select.Option value="FAIL_RETRY">
      {t('application.failureStrategyFailRetry')}
    </Select.Option>
    <Select.Option value="FAIL_OVER">
      {t('application.failureStrategyFailOver')}
    </Select.Option>
  </Select>
</Form.Item>
```

`useEffect` 初始化中：编辑时 `failureStrategy: application.failureStrategy`，新建时 `failureStrategy: 'FAIL_RETRY'`。加 `import { Select } from 'antd';`。

- [x] **Step 3: locales 添加三策略文案**

在 `applications` 命名空间 locale 文件中添加：
- `failureStrategy`: "失败处理策略"
- `failureStrategyHelp`: "控制渠道故障时转移行为：快速失败/同渠道换 Key/换渠道转移"
- `failureStrategyPlaceholder`: "请选择失败处理策略"
- `failureStrategyFailFast`: "快速失败（首个 Key 失败即抛错）"
- `failureStrategyFailRetry`: "失败重试（同渠道换 Key，不换渠道）"
- `failureStrategyFailOver`: "失败转移（换 Key + 换渠道）"

英文版同步添加。

- [x] **Step 4: 类型检查与构建**

Run: `cd gateway-console && npx tsc --noEmit && npm run build`
Expected: 无错误。

- [x] **Step 5: 提交**

```bash
git add gateway-console
git commit -m "feat(console): ApplicationFormModal 加失败处理策略配置

应用创建/编辑表单新增 failureStrategy 下拉（FAIL_FAST/FAIL_RETRY/
FAIL_OVER），默认 FAIL_RETRY。types/application.ts 加 FailureStrategy
类型，locales 适配三策略文案。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 11: 端点熔断应急 UI（Channels 页）

**Files:**
- Modify: `gateway-console/src/pages/Channels/`（端点列表展示组件，定位实际文件）
- Modify: `gateway-console/src/locales/{zh,en}/channels.json`

**说明：** 后端 `ChannelController` 已有 force-open/force-close/state 端点，`resilienceApi.circuitBreaker` 与 `useForceOpenCircuitBreaker`/`useForceCloseCircuitBreaker`/`useCircuitBreakerState` hooks 已封装。本任务在 Channels 页端点维度接入这些 hooks。

- [x] **Step 1: 定位端点展示组件**

Run: `grep -rn "ChannelEndpointResponse\|endpoints" gateway-console/src/pages/Channels --include="*.tsx" -l`
定位渲染渠道端点列表的组件（可能在 ChannelCreateWizard 或渠道详情抽屉中）。

- [x] **Step 2: 端点行新增熔断状态 Tag + 应急按钮**

在端点列表每行新增：
- 熔断状态 Tag（调 `useCircuitBreakerState(channelId, endpointId)`，展示 CLOSED/OPEN/HALF_OPEN）
- "强制熔断"按钮（调 `useForceOpenCircuitBreaker`，二次确认）
- "强制恢复"按钮（调 `useForceCloseCircuitBreaker`）

```tsx
import { useCircuitBreakerState, useForceOpenCircuitBreaker, useForceCloseCircuitBreaker } from '@/services/query/useResilience';

// 端点行内：
const { data: cbState } = useCircuitBreakerState(channel.id, endpoint.id);
const forceOpen = useForceOpenCircuitBreaker();
const forceClose = useForceCloseCircuitBreaker();

<Tag color={cbState?.state === 'CLOSED' ? 'green' : cbState?.state === 'OPEN' ? 'red' : 'orange'}>
  {cbState?.state ?? '-'}
</Tag>
<Button size="small" danger onClick={() => forceOpen.mutate({ channelId: channel.id, endpointId: endpoint.id })}>
  {t('channels.forceOpen')}
</Button>
<Button size="small" onClick={() => forceClose.mutate({ channelId: channel.id, endpointId: endpoint.id })}>
  {t('channels.forceClose')}
</Button>
```

- [x] **Step 3: locales 适配**

`channels` 命名空间添加：`forceOpen`: "强制熔断"、`forceClose`: "强制恢复"、`circuitBreakerState`: "熔断状态"、`forceOpenConfirm`: "确认强制熔断该端点？将立即切断流量。"。英文同步。

- [x] **Step 4: 类型检查与构建**

Run: `cd gateway-console && npx tsc --noEmit && npm run build`
Expected: 无错误。

- [x] **Step 5: 提交**

```bash
git add gateway-console
git commit -m "feat(console): Channels 页端点维度熔断应急 UI

端点列表每行新增熔断状态展示与 forceOpen/forceClose 应急按钮，
对接既有 resilienceApi.circuitBreaker 与 hooks。locales 适配文案。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 12: 容灾总览页重组

**Files:**
- Modify: `gateway-console/src/pages/resilience/overview/index.tsx`
- Modify: `gateway-console/src/pages/resilience/overview/eventDisplay.ts`
- Modify: `gateway-console/src/locales/{zh,en}/resilience.json`

**说明：** 删除 Cluster 拓扑卡片区块 + 共因跳过列，新增端点熔断状态大盘区块。总览页 = 耗尽告警 + 转移事件流 + 端点熔断状态大盘。

- [x] **Step 1: 删除 ClusterCard 组件与拓扑区块**

`pages/resilience/overview/index.tsx`：
- 删除 `ClusterCard` 函数（整段）
- 删除 `useClusters`、`useAllChannels`、`groupChannelsByCluster` import 与调用
- 删除"故障域拓扑" Card 区块（含 Row/Col/ClusterCard 渲染）
- 删除 `Cluster`/`Channel` type import

- [x] **Step 2: 转移事件流表格删除共因跳过列**

`FailoverEventTable` 的 columns 删除 `commonCauseSkip` 列定义。移除 `WarningOutlined` import（若仅此处用）。

- [x] **Step 3: 新增端点熔断状态大盘区块**

在耗尽告警与转移事件流之间新增"端点熔断状态" Card：

```tsx
{/* 端点熔断状态大盘 */}
<Card title={t('overview.circuitBreakerDashboard')} style={{ marginBottom: 16 }}>
  <CircuitBreakerDashboard t={t} />
</Card>
```

新增 `CircuitBreakerDashboard` 组件：拉取全部渠道端点（`useAllChannels`），逐端点调 `useCircuitBreakerState` 展示状态 Tag + 应急操作入口。或简化为表格：渠道名 / 端点 URL / 协议 / 熔断状态 / 应急按钮。

- [x] **Step 4: locales 适配**

`resilience` 命名空间：删除 `clusterTopology`/`clusterTopologyHelp`/`noClusters`/`members`/`cluster.providerId`/`commonCauseSkip`/`commonCauseSkipHelp` 等 Cluster 相关 key。新增 `circuitBreakerDashboard`: "端点熔断状态"、`circuitBreakerDashboardHelp`: "各端点熔断器当前状态与应急操作"。英文同步。

- [x] **Step 5: 适配 eventDisplay.ts**

若 `eventDisplay.ts` 引用共因跳过相关逻辑，清理。`formatRoute` 中若处理 clusterId，删除。

- [x] **Step 6: 适配总览页测试**

`pages/resilience/overview/__tests__/` 下测试：删除 Cluster 拓扑与共因跳过相关断言，新增端点熔断大盘渲染测试。

- [x] **Step 7: 类型检查、构建与测试**

Run: `cd gateway-console && npx tsc --noEmit && npm run build && npx vitest run`
Expected: 全部通过。

- [x] **Step 8: 提交**

```bash
git add gateway-console
git commit -m "refactor(console): 容灾总览页重组

删除 Cluster 拓扑卡片与共因跳过列，新增端点熔断状态大盘区块。
总览页 = 耗尽告警 + 端点熔断状态 + 转移事件流。locales 清理
Cluster 文案、新增熔断大盘文案。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 13: spec 同步与文档更新

**Files:**
- Delete: `openspec/specs/cluster-failover/` 目录（归档时删主 spec）
- Modify: `openspec/specs/application/` — 加 failureStrategy 字段说明
- Modify: `openspec/specs/channel-failover/` — L1 按策略控制，删共因跳过 requirement
- Modify: `openspec/specs/resilience-console/` — 总览页重组，加端点熔断应急
- Modify: `docs/容灾方案设计.md` / `docs/容灾管理范式.md`（若存在）

- [x] **Step 1: cluster-failover capability spec 整体退场**

```bash
rm -rf openspec/specs/cluster-failover/
```
（若该目录不存在，跳过——可能已在归档时删除。）

- [x] **Step 2: 更新 application spec**

`openspec/specs/application/` 下 spec：新增 failureStrategy 字段 requirement（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一，默认 FAIL_RETRY）。

- [x] **Step 3: 更新 channel-failover spec**

`openspec/specs/channel-failover/`：删除共因跳过 requirement，改为"L1 转移按 ApplicationChannel.priority 顺序 + 应用 failureStrategy 控制 L0/L1"。删除转移事件 clusterId/commonCauseSkip 字段说明。

- [x] **Step 4: 更新 resilience-console spec**

`openspec/specs/resilience-console/`：删除 Cluster 拓扑展示 requirement，新增端点熔断应急操作 UI + 端点熔断状态大盘 + 应用策略配置 requirement。

- [x] **Step 5: 更新 docs 文档**

`docs/容灾方案设计.md` 与 `docs/容灾管理范式.md`（若存在）：删除 Cluster/共因跳过章节，新增应用级失败处理策略章节，说明容灾由策略 + priority + 熔断器承担。

- [x] **Step 6: grep 全仓残留确认**

```bash
grep -rn "Cluster\|clusterId\|commonCauseSkip\|ClusterHealthAggregator\|ClusterAffinityRouter" \
  --include="*.java" --include="*.ts" --include="*.tsx" --include="*.md" \
  gateway-boot/src openspec docs gateway-console/src
```
Expected: 仅 spec 归档目录与历史文档中可能残留（可接受），源码中无残留。

- [x] **Step 7: 提交**

```bash
git add openspec docs
git commit -m "docs(resilience): spec 与文档同步容灾重构

cluster-failover capability spec 退场；application spec 加 failureStrategy；
channel-failover spec 删共因跳过改策略控制；resilience-console spec 加
端点熔断应急与总览页重组。docs 文档同步更新。

Co-Authored-By: Claude <noreply@anthropic.com>"
```

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## Task 14: 全链路回归

**Files:** 无修改，仅验证

- [x] **Step 1: 后端全量测试**

Run: `./mvnw -pl gateway-boot -am test`
Expected: BUILD SUCCESS，所有测试通过。核心服务层覆盖率满足约束（≥90%）。

- [x] **Step 2: 前端构建**

Run: `cd gateway-console && npm run build`
Expected: 构建成功，tsc 类型检查通过。

- [x] **Step 3: 前端单元测试**

Run: `cd gateway-console && npx vitest run`
Expected: 所有测试通过。

- [x] **Step 4: 端到端验证（手动）**

启动应用，验证以下场景：
1. **FAIL_FAST 行为**：配置应用策略为 FAIL_FAST，触发首个 Key 失败，验证立即抛错不换 Key 不换渠道
2. **FAIL_RETRY 行为**：配置应用策略为 FAIL_RETRY（默认），触发 Key 失败，验证同渠道换 Key 但不换渠道
3. **FAIL_OVER 行为**：配置应用策略为 FAIL_OVER，触发渠道失败，验证换到下一渠道
4. **端点熔断应急**：Channels 页点"强制熔断"端点，验证流量切断；点"强制恢复"验证恢复
5. **priority 顺序转移**：FAIL_OVER 下验证按 ApplicationChannel.priority 顺序转移
6. **跨供应商转移**：配置跨供应商渠道，验证 FAIL_OVER 跨供应商转移
7. **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为一致

- [x] **Step 5: 确认现有配置 UI 完整（tasks.md Task 8）**

验证 Application 渠道 priority 配置（ChannelManageModal）、Application timeout 配置、渠道健康状态展示功能完整可用。

- [x] **Step 6: 提交回归报告（可选）**

若有测试修复或文档补充，提交。否则标记回归通过。

archived-with: 2026-07-05-refactor-resilience-to-application-strategy
---

## 自审（Self-Review）

### 1. Spec 覆盖检查

对照设计文档与 tasks.md 逐项核对：

| tasks.md 项 | 对应 Task | 覆盖 |
|-------------|----------|------|
| 1.1-1.11 删 Cluster + 共因跳过 | Task 1（Cluster 全套）+ Task 2（共因跳过行为）+ Task 3（字段全链路）+ Task 4（grep） | ✅ |
| 2.1-2.10 应用级失败处理策略 | Task 5（枚举+Application）+ Task 6（RoutingContext 透传）+ Task 7（Invoker 按策略 TDD） | ✅ |
| 3.1-3.6 Flyway | Task 8（V65-V68） | ✅ |
| 4.1-4.3 端点熔断应急 UI | Task 11 | ✅ |
| 5.1-5.4 策略配置 UI | Task 10 | ✅ |
| 6.1-6.4 总览页重组 | Task 12 | ✅ |
| 7.1-7.4 前端 Cluster 清除 | Task 9 | ✅ |
| 8.1-8.3 现有配置 UI 完整 | Task 14 Step 5 | ✅ |
| 9.1-9.3 spec 与文档 | Task 13 | ✅ |
| 10.1-10.4 全链路回归 | Task 14 | ✅ |

### 2. 占位符扫描

- 无 "TBD"/"TODO"/"fill in details"
- 删除任务给出确切文件与行号
- 新增任务给出完整代码
- 测试任务给出完整测试代码骨架

### 3. 类型一致性

- `FailureStrategy` 枚举：FAIL_FAST/FAIL_RETRY/FAIL_OVER — 全计划统一
- `RoutingContext.failureStrategy()` — Task 6 定义，Task 7 使用，签名一致
- `KeyFailoverInvoker.invokeSingleKey(RoutingContext, ProtocolRequest)` — Task 7 定义与调用一致
- `publishFailoverEvent` 签名：Task 2 保留 commonCauseSkip 传 false，Task 3 删除参数 — 两阶段过渡一致
- 前端 `FailureStrategy` 类型与后端枚举名一致

### 4. 编译安全性

- Task 1（删 Cluster）：Channel.clusterId 是物理 ID 无 FK，删 Cluster 不影响编译
- Task 2（删共因跳过）：保留字段定义，仅删行为，编译通过
- Task 3（删字段）：横切一次性删除所有引用，编译通过
- Task 5-7（加法）：独立于减法分支，Task 6 改 RoutingContext 字段需在 Task 3 删 clusterId 之后（依赖已标注）
- Task 8（Flyway）：独立于代码，可并行
