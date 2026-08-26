# Comet Design Handoff

- Change: refactor-resilience-to-application-strategy
- Phase: design
- Mode: compact
- Context hash: ce67dd0f49338e76959ba6260f956b3a8ec68b6642211f7d0f64c90275f41603

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/refactor-resilience-to-application-strategy/proposal.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/proposal.md
- Lines: 1-61
- SHA256: f8fcb20c7003a9f5125086a2c810734d0cd72836d0b908230a804d3f74433659

```md
## Why

当前容灾模型中，Cluster（故障域根实体）与 Application 在「控制路由走向」上职责交叉：Application 通过 `ApplicationChannel.priority` 配置转移顺序，Cluster 通过 `Channel.clusterId` 驱动共因跳过，两者独立配置、彼此不可见，语义会冲突（应用配的顺序可能被共因跳过覆盖）。且 Cluster 共因跳过会误杀不共因的候选（如同供应商不同账户的多 Key，账户额度独立、不共因，却被同 clusterId 跳过）。经场景验证（研发自动化同供应商多 Key、OpenAI 官方+Azure 跨供应商共因），共因跳过的收益（首次故障省几次失败尝试）配不上其复杂度，且熔断器已覆盖持续故障的痛感。

同时，不同下游应用场景（流程自动化/研发自动化/AGI/BI）对失败处理的诉求差异大（BI 愿快速失败省成本、流程自动化要转移保可用、研发自动化要同渠道换 Key），当前无应用级失败处理策略，无法表达场景差异。

故删除 Cluster 与共因跳过，引入轻量应用级失败处理策略（三选一），容灾完全由 Application 承担。

## What Changes

**删除（减法）**：
- **BREAKING** 删除 Cluster 故障域根实体全套：实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- **BREAKING** 删除 `Channel.clusterId` 字段、`RoutingContext.clusterId` 字段
- **BREAKING** 删除 `ChannelFailoverInvoker` 共因跳过逻辑（`commonCauseFailedClusters` + 跳过判定 + `publishFailoverEvent` 的 `commonCauseSkip` 参数）
- **BREAKING** 删除 `FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段
- **BREAKING** 删除 `FailoverEventGateway.findRecent` 的 clusterId 过滤参数
- **BREAKING** Flyway 迁移：删 `clusters` 表、`channels.cluster_id` 列、`failover_events.from_cluster_id`/`to_cluster_id`/`common_cause_skip` 列
- 删除前端 Cluster 拓扑卡片 + 共因跳过展示 + `grouping.ts`
- **BREAKING** `cluster-failover` capability 整体退场

**新增（加法）**：
- 引入应用级失败处理策略枚举（三选一互斥，挂 Application，轻量单字段，不独立实体）：
  - `FAIL_FAST`（快速失败）：L0 不跑、L1 不跑，第一个 Key 失败立即抛错
  - `FAIL_OVER`（失败转移）：L0 跑、L1 跑，同渠道换 Key + 换渠道，全耗尽抛错（当前行为）
  - `FAIL_RETRY`（失败重试）：L0 跑、L1 不跑，同渠道内换 Key，不换渠道
- 默认策略：`FAIL_RETRY`（失败重试）——契合「同供应商多 Key」主场景，K1 限流换 K2
- **BREAKING** 现有应用数据迁移：设为 `FAIL_OVER`（保持原行为不变）
- `ChannelFailoverInvoker` 按应用策略控制 L0/L1 行为

**补齐管理员容灾管理前端功能**：
- 端点熔断应急操作 UI（forceOpen/forceClose + 状态展示）
- 端点熔断状态大盘（容灾总览页）
- 容灾总览页重组：删 Cluster 拓扑后 = 转移事件流 + 耗尽告警 + 端点熔断状态大盘
- 应用失败处理策略配置 UI（ApplicationFormModal 加策略选择）
- 确保 Application 渠道 priority 配置 UI 完整可用

**非目标**：
- 不做共因跳过（无论 Cluster、共因组字段、providerId 判定均不做）
- 不做场景模板（三策略已覆盖场景差异，模板 YAGNI）
- 不做下游应用请求级选择渠道分组
- 不恢复 ResilienceProfile / L2 降级

## Capabilities

### New Capabilities
- `application-failure-strategy`: 应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），控制 L0/L1 故障转移行为

### Modified Capabilities
- `application`: Application 实体新增失败处理策略字段
- `channel-failover`: 删除 L1 共因跳过 requirement 与转移事件 clusterId/commonCauseSkip 字段，L1 转移改为按 ApplicationChannel.priority 顺序 + 应用策略控制 L0/L1
- `resilience-console`: 删除 Cluster 拓扑展示与共因跳过列，新增端点熔断应急操作 UI 与端点熔断状态大盘，应用管理页加失败处理策略配置，容灾总览页重组

## Impact

- **后端代码**：`domain/resilience`（Cluster 整删、FailoverEvent 瘦身）、`domain/application`（Application 加策略字段）、`domain/supply`（Channel 删 clusterId、RoutingContext 删 clusterId）、`application/proxy`（ChannelFailoverInvoker 按策略控制 L0/L1、删共因跳过、RoutingResolver 删 clusterId 填充）、`application/resilience`（ClusterService 整删）、`adapter/api`（ClusterController 整删、ApplicationController 策略字段）、`infrastructure/resilience`（Cluster Gateway 整删）
- **DB schema**：删 clusters 表、channels.cluster_id 列、failover_events 相关列；applications 表加策略字段；数据迁移现有应用策略=FAIL_OVER
- **前端**：gateway-console 容灾总览页重组、应用策略配置 UI、端点熔断 UI 新增、Cluster 相关清除
- **API**：**BREAKING** `/api/v1/resilience/clusters` 整删；转移事件查询响应字段调整；Application 创建/更新/查询含策略字段
- **依赖**：复用既有 `KeyFailoverInvoker`（L0）、`ChannelEndpointCircuitBreakerManager`（熔断应急）
- **双 API 兼容**：`/v1/chat/completions` 与 `/v1/messages` 行为保持
- **回头路警示**：应用级策略仅为 Application 上单枚举字段，不得演变为已删的 ResilienceProfile（独立实体+全局解析链+L2/PinnedModel/会话亲和）
```

## openspec/changes/refactor-resilience-to-application-strategy/design.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/design.md
- Lines: 1-72
- SHA256: 55ffccae31ec2432d7a5d3dbe9e64953307c3aa92ecb7f45fda6e87dc0d2234b

```md
## Context

刚归档的 `simplify-resilience-architecture` change 建立了 Cluster 故障域 + L1 共因跳过机制。但实践中发现两个问题：

1. **职责交叉**：Cluster（渠道侧全局共因分组）与 Application（应用侧 priority 转移顺序）都在影响故障转移走向，配置入口分离、彼此不可见，应用配的顺序可能被共因跳过覆盖。

2. **共因跳过误杀**：同供应商不同账户的多 Key（如 OpenAI 账户甲的 k1/k2 与账户乙的 k3/k4）账户额度独立、故障不共因，但若被归同一 clusterId，共因跳过会误杀可用候选。

经场景验证（研发自动化同供应商多 Key、OpenAI 官方+Azure 跨供应商底层共因），确认：
- 共因跳过的收益（首次故障省几次失败尝试）配不上其复杂度
- 既有端点级熔断器已覆盖持续故障的痛感（连续失败后 OPEN，后续跳过）
- 共因渠道首次故障多试一次的代价可接受

同时，不同下游场景对失败处理诉求差异大（BI 愿快速失败、流程自动化要转移、研发自动化要同渠道换 Key），需引入应用级失败处理策略表达场景差异。

## Goals / Non-Goals

**Goals:**
- 删除 Cluster 故障域根实体与共因跳过，消除与 Application 的职责交叉
- 引入轻量应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），支持场景差异化
- 容灾走向由 Application 策略 + ApplicationChannel.priority 顺序 + 端点级熔断器承担
- 补齐管理员容灾管理前端功能：端点熔断应急 UI、熔断状态大盘、总览页重组、策略配置
- 确保 Application 渠道 priority/timeout 配置 UI 完整可用

**Non-Goals:**
- 不做共因跳过（无论 Cluster、共因组字段、providerId 判定均不做）
- 不做场景模板（三策略已覆盖场景差异）
- 不做下游应用请求级选择渠道分组
- 不恢复 ResilienceProfile / L2 降级

## Decisions

### D1: 容灾配置收敛到 Application
Cluster 退场，容灾走向完全由 Application 决定（授权哪些渠道 + priority 顺序 + 失败处理策略 + timeout）。

### D2: 不做共因跳过
故障时不做共因跳过。共因渠道（如 OpenAI 官方+Azure）首次故障多试一次，由端点级熔断器在连续失败后 OPEN 跳过。避免误杀不共因候选（同供应商不同账户 Key）。

### D3: 应用级失败处理策略（三选一互斥）
Application 新增 `failureStrategy` 枚举字段（轻量单字段，不独立实体，不走 ResilienceProfile 回头路）：

| 策略 | L0 同渠道换 Key | L1 换渠道 | 行为 |
|------|----------------|----------|------|
| `FAIL_FAST`（快速失败） | ❌ | ❌ | 第一个 Key 失败立即抛错 |
| `FAIL_OVER`（失败转移） | ✅ | ✅ | Key 用完换下一渠道，全耗尽抛错 |
| `FAIL_RETRY`（失败重试） | ✅ | ❌ | 同渠道内换 Key，不换渠道 |

- 三者递进：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER（L0/L1 逐级启用）
- `ChannelFailoverInvoker` 按应用策略控制是否跑 L0（KeyFailoverInvoker）、是否跑 L1（换渠道）
- 默认 `FAIL_RETRY`（契合「同供应商多 Key」主场景，K1 限流换 K2）

### D4: 数据迁移保持现有应用行为
当前所有应用行为是 FAIL_OVER（L0+L1 都跑）。改默认为 FAIL_RETRY 后，数据迁移把现有应用 `failureStrategy` 设为 `FAIL_OVER`，保持行为不变。新应用默认 FAIL_RETRY。

### D5: 端点熔断器作为故障跳过的补充机制
既有 `ChannelEndpointCircuitBreakerManager`（端点级熔断器）作为故障时跳过端点的补充机制。端点连续失败 → OPEN → 后续请求跳过。管理员可手动 forceOpen/forceClose 应急。与策略正交：策略控制候选间转移，熔断器控制端点级跳过。

### D6: 端点熔断管理前端
补 forceOpen/forceClose 应急操作 UI + 熔断状态大盘：
- Channels 页端点维度：单端点熔断/恢复操作 + 状态展示
- 容灾总览页：端点熔断状态大盘区块
后端 API 已有，仅前端补 UI。

### D7: 容灾总览页重组
删 Cluster 拓扑卡片 + `grouping.ts` + 转移事件流「共因跳过」列后，总览页 = 转移事件流（删 clusterId/commonCauseSkip）+ 耗尽告警 + 端点熔断状态大盘。

## Risks / Trade-offs

- **默认 FAIL_RETRY 的代价**：渠道全 Key 失败时，FAIL_RETRY 会试完同渠道所有 Key 才抛错（不换渠道），比 FAIL_OVER 多耗 (Key数-1)×RTT？——实际不会，FAIL_OVER 也会试完同渠道 Key（L0 跑）才换渠道，两者在渠道内代价相同；区别只在渠道全失败后 FAIL_RETRY 抛错、FAIL_OVER 换渠道。
- **共因跳过删除的延迟代价**：跨供应商共因渠道首次故障多试 1 次，熔断器 OPEN 后缓解。
- **BREAKING 影响**：Cluster 端点整删、转移事件字段变更、failover_events 表列删除、Application 默认策略变更（现有应用迁移 FAIL_OVER）。
- **回头路风险**：应用级策略仅为单枚举字段，须严格不演变为 ResilienceProfile。
```

## openspec/changes/refactor-resilience-to-application-strategy/tasks.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/tasks.md
- Lines: 1-90
- SHA256: 2e2d115826a0c2d55cf8c5ad666b72d2a4a46a34366dbefe08590eaba99bcb14

[TRUNCATED]

```md
# 实施任务清单

> change: refactor-resilience-to-application-strategy
> 范围：删 Cluster + 共因跳过（减法）+ 引入应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY）+ 补管理员容灾管理前端功能。
> 命名统一：FAIL_FAST（快速失败）/ FAIL_OVER（失败转移）/ FAIL_RETRY（失败重试），三选一互斥，默认 FAIL_RETRY。

## 1. 删除 Cluster 与共因跳过（减法，后端）

- [ ] 1.1 删除 Cluster 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- [ ] 1.2 删除 Channel.clusterId 字段 + ChannelDo/Repository 适配
- [ ] 1.3 删除 RoutingContext.clusterId 字段 + 所有构造点适配
- [ ] 1.4 删除 RoutingResolver.buildContext 的 clusterId 填充
- [ ] 1.5 删除 ChannelFailoverInvoker 共因跳过逻辑（commonCauseFailedClusters + 跳过判定）
- [ ] 1.6 删除 FailoverOccurredEvent/FailoverEvent/FailoverEventDo/FailoverEventResponse 的 commonCauseSkip + fromClusterId/toClusterId 字段
- [ ] 1.7 删除 FailoverEventGatewayImpl.toEntity/toDataObject 相关字段透传 + findRecent 的 clusterId 过滤参数
- [ ] 1.8 删除 FailoverEventListener.toEntity 的 commonCauseSkip 透传
- [ ] 1.9 删除 publishFailoverEvent 的 commonCauseSkip 参数（保留 FailoverDecision.valueOf 容错）
- [ ] 1.10 适配 ResilienceEventController/Service 的查询 API（删 clusterId 过滤）
- [ ] 1.11 grep 确认无 Cluster/clusterId/commonCauseSkip 代码残留

## 2. 应用级失败处理策略（加法，后端）

- [ ] 2.1 创建 FailureStrategy 枚举（FAIL_FAST/FAIL_OVER/FAIL_RETRY）
- [ ] 2.2 Application 实体加 failureStrategy 字段 + ApplicationDo 适配
- [ ] 2.3 ApplicationRequest/ApplicationResponse 加 failureStrategy 字段
- [ ] 2.4 ApplicationServiceImpl create/update/toResponse 透传 failureStrategy（默认 FAIL_RETRY）
- [ ] 2.5 ChannelFailoverInvoker 按应用 failureStrategy 控制 L0/L1 行为：
  - FAIL_FAST：候选首个 Key 失败立即抛错（不调 KeyFailoverInvoker 换 Key、不换渠道）
  - FAIL_RETRY：L0 跑（KeyFailoverInvoker 换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错
  - FAIL_OVER：L0 跑 + L1 跑（按 priority 换渠道），全耗尽抛错
- [ ] 2.6 Invoker 需获取应用 failureStrategy（经 ApplicationGateway 或 RoutingRequest 透传）
- [ ] 2.7 TDD：FAIL_FAST 首个 Key 失败立即抛错
- [ ] 2.8 TDD：FAIL_RETRY 同渠道换 Key 不换渠道
- [ ] 2.9 TDD：FAIL_OVER 换渠道全耗尽抛错
- [ ] 2.10 TDD：默认策略 FAIL_RETRY（未指定时）

## 3. Flyway 迁移

- [ ] 3.1 Flyway V65：DROP TABLE clusters
- [ ] 3.2 Flyway V66：ALTER TABLE channels DROP COLUMN cluster_id
- [ ] 3.3 Flyway V67：ALTER TABLE failover_events DROP COLUMN from_cluster_id, to_cluster_id, common_cause_skip
- [ ] 3.4 Flyway V68：ALTER TABLE applications ADD COLUMN failure_strategy VARCHAR NOT NULL DEFAULT 'FAIL_RETRY'
- [ ] 3.5 Flyway V68：数据迁移 UPDATE applications SET failure_strategy='FAIL_OVER'（现有应用保持原行为）
- [ ] 3.6 确认 H2/PG 兼容（IF EXISTS），测试 profile 适配

## 4. 端点熔断应急 UI（前端）

- [ ] 4.1 前端 Channels 页端点维度：forceOpen/forceClose 按钮 + 状态展示
- [ ] 4.2 前端 types/services：熔断应急 API 接 UI（resilienceApi.circuitBreaker 已封装）
- [ ] 4.3 前端 locales 适配（熔断操作文案）

## 5. 应用失败处理策略配置 UI（前端）

- [ ] 5.1 前端 types/application.ts 加 failureStrategy 类型
- [ ] 5.2 前端 ApplicationFormModal 加策略选择（FAIL_FAST/FAIL_OVER/FAIL_RETRY 下拉）
- [ ] 5.3 前端 applicationApi 请求体含 failureStrategy
- [ ] 5.4 前端 locales 适配（三策略文案）

## 6. 容灾总览页重组（前端）

- [ ] 6.1 删除 Cluster 拓扑卡片 + grouping.ts
- [ ] 6.2 删除转移事件流表格的共因跳过列 + clusterId 展示
- [ ] 6.3 新增端点熔断状态大盘区块（各端点熔断器状态 + 应急操作入口）
- [ ] 6.4 总览页重组：转移事件流 + 耗尽告警 + 端点熔断状态大盘

## 7. 前端 Cluster 相关清除

- [ ] 7.1 删除 types/resilience.ts 中 Cluster/ClusterRequest 定义
- [ ] 7.2 删除 resilienceApi.clusters CRUD
- [ ] 7.3 删除 useClusters hook
- [ ] 7.4 grep 确认前端无 Cluster 残留

## 8. 确保现有配置 UI 完整

- [ ] 8.1 验证 Application 渠道 priority 配置（ChannelManageModal）功能完整
- [ ] 8.2 验证 Application timeout 配置功能完整
- [ ] 8.3 验证渠道健康状态展示完整

## 9. spec 同步与文档

```

Full source: openspec/changes/refactor-resilience-to-application-strategy/tasks.md

## openspec/changes/refactor-resilience-to-application-strategy/specs/application-failure-strategy/spec.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/specs/application-failure-strategy/spec.md
- Lines: 1-45
- SHA256: a5056b15bfa5b2e9d8a82d23cced1f279f5d2c9f7cefcee1b9ab4757dc789e46

```md
# Application Failure Strategy Delta Spec

> 新增能力：应用级失败处理策略。Application 通过单枚举字段配置失败处理模式（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），控制 L0/L1 故障转移行为，支持下游场景差异化（BI 快速失败、流程自动化失败转移、研发自动化失败重试）。

## ADDED Requirements

### Requirement: 应用级失败处理策略

系统 SHALL 在 Application 根实体上提供 `failureStrategy` 枚举字段（轻量单字段，不独立实体），承载该应用的失败处理模式。策略 SHALL 为三选一互斥：

- `FAIL_FAST`（快速失败）：第一个 Key 失败立即抛错，L0（同渠道换 Key）与 L1（换渠道）均不跑
- `FAIL_OVER`（失败转移）：L0 跑（同渠道换 Key）+ L1 跑（换渠道），全候选耗尽抛错
- `FAIL_RETRY`（失败重试）：L0 跑（同渠道换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错

`ChannelFailoverInvoker` SHALL 按候选所属应用的 `failureStrategy` 控制 L0/L1 行为：
- `FAIL_FAST`：候选首个 Key 失败立即抛错
- `FAIL_RETRY`：候选内 L0 换 Key 试完失败后抛错，不试下一渠道
- `FAIL_OVER`：候选内 L0 换 Key 试完失败后，按 `ApplicationChannel.priority` 试下一渠道，全耗尽抛错

默认值 SHALL 为 `FAIL_RETRY`。现有应用数据迁移 SHALL 设为 `FAIL_OVER`（保持原行为）。

#### Scenario: 快速失败策略

- **WHEN** 应用 `failureStrategy=FAIL_FAST`，候选首个 Key 调用失败
- **THEN** `ChannelFailoverInvoker` SHALL 立即抛出异常
- **THEN** 系统 SHALL NOT 试同渠道其他 Key，SHALL NOT 试下一渠道

#### Scenario: 失败重试策略（默认）

- **WHEN** 应用 `failureStrategy=FAIL_RETRY`，候选 Key-A 失败
- **THEN** `ChannelFailoverInvoker` SHALL 试同渠道其他 Key（L0）
- **THEN** 同渠道所有 Key 失败时 SHALL 抛出异常
- **THEN** 系统 SHALL NOT 试下一渠道（L1 不跑）

#### Scenario: 失败转移策略

- **WHEN** 应用 `failureStrategy=FAIL_OVER`，候选渠道所有 Key 失败
- **THEN** `ChannelFailoverInvoker` SHALL 按 `ApplicationChannel.priority` 试下一渠道
- **THEN** 所有渠道全耗尽时 SHALL 抛出最后异常

#### Scenario: 现有应用迁移保持行为

- **WHEN** 数据迁移执行
- **THEN** 现有应用 `failureStrategy` SHALL 设为 `FAIL_OVER`
- **THEN** 现有应用容灾行为 SHALL 保持不变（L0+L1 均跑）
```

## openspec/changes/refactor-resilience-to-application-strategy/specs/application/spec.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/specs/application/spec.md
- Lines: 1-36
- SHA256: 3307ac648c693cda9983156a669fea99215e15d9653992e984f2f63fc7f4c054

```md
# Application Delta Spec

## MODIFIED Requirements

### Requirement: Application 根实体实体

系统 SHALL 提供 `Application` 根实体实体作为「权限 + 行为」双根实体，承载 N 把 Key 的应用归属、渠道可见性、应用级超时、失败处理策略，并预留配额/看板字段。

**实体字段**（保留 timeout，新增 failureStrategy）:
- `code` — 应用编码，全局唯一
- `name` — 应用名称
- `description` — 应用描述
- `state` — 应用生命周期状态（`ApplicationState`）
- `timeout` — 请求超时秒数（0 表示用渠道默认）
- `failureStrategy` — 失败处理策略枚举（`FAIL_FAST`/`FAIL_OVER`/`FAIL_RETRY`，默认 `FAIL_RETRY`，详见 application-failure-strategy capability）
- `quotaBudgetId` — 配额预算 ID（预留）
- `dashboardId` — 看板 ID（预留）
- 审计字段 `createdBy/createdAt/updatedBy/updatedAt` 继承自 `BaseEntity`

**API**:
- `POST /api/v1/applications` — 创建应用（请求体 `code/name/description/timeout/failureStrategy`，返回 `ApplicationResponse`，HTTP 201）
- `PUT /api/v1/applications/{id}` — 更新应用（含 failureStrategy）
- `GET /api/v1/applications/{id}` — 查询应用详情（含 failureStrategy）
- `GET /api/v1/applications` — 查询全部应用列表
- `DELETE /api/v1/applications/{id}` — 删除应用（HTTP 204）

#### Scenario: 创建应用含失败处理策略

- **WHEN** 管理员调用 `POST /api/v1/applications` 传入 `code/name/description/timeout/failureStrategy`
- **THEN** 系统 SHALL 创建 `Application` 记录，`code` 全局唯一
- **THEN** 系统 SHALL 返回 HTTP 201 与创建后的 `ApplicationResponse`

#### Scenario: 未指定策略默认失败重试

- **WHEN** 创建应用未传 `failureStrategy`
- **THEN** 系统 SHALL 将 `failureStrategy` 设为默认值 `FAIL_RETRY`
```

## openspec/changes/refactor-resilience-to-application-strategy/specs/channel-failover/spec.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/specs/channel-failover/spec.md
- Lines: 1-59
- SHA256: 2c2c0505224d98f5ebebf256c665692790672ba497b5e96a09fb44444178dea2

```md
# Channel Failover Delta Spec

## MODIFIED Requirements

### Requirement: ChannelFailoverInvoker L1 运行时失败转移回路

系统 SHALL 提供 `ChannelFailoverInvoker` 作为 Channel 级（L1）运行时失败转移回路，按候选所属应用的 `failureStrategy` 控制 L0/L1 行为，作为主转移路径。

**三层容灾栈**（删除 L2 模型级 + 删除 clusterId 共因跳过）:
- L0 Key 级（同渠道换 Key，复用 `KeyFailoverInvoker`）
- L1 Channel 级（同模型按 ApplicationChannel.priority 顺序换渠道）
- L3 抛错

**调用入口**:
- `invoke(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId)` — 非流式
- `invokeStream(RoutingContext primary, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, String traceId, StreamCallback callback)` — 流式

**每候选独立调谐**：试每个候选前 SHALL 基于原始请求派生副本，用该候选的 `RoutingContext` 独立调谐，不携带前一候选痕迹。

**策略驱动的 L0/L1 行为**（详见 application-failure-strategy capability）:
- `FAIL_FAST`：候选首个 Key 失败立即抛错，L0/L1 均不跑
- `FAIL_RETRY`（默认）：L0 跑（同渠道换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错
- `FAIL_OVER`：L0 跑 + L1 跑（按 priority 换渠道），全耗尽抛错

**故障跳过机制**：L1 不做共因跳过。由端点级熔断器（`ChannelEndpointCircuitBreakerManager`）在端点连续失败 OPEN 后跳过该端点，不引入共因分组。

#### Scenario: L1 候选内逐个尝试（FAIL_OVER 策略）

- **WHEN** 应用 `failureStrategy=FAIL_OVER`，`ChannelFailoverInvoker.invoke` 接收按 ApplicationChannel.priority 排序的候选列表
- **THEN** 系统 SHALL 对每个候选依次调用 `KeyFailoverInvoker.invoke`（L0 Key 级转移）
- **THEN** 某候选成功时 SHALL 立即返回响应

#### Scenario: L1 候选失败按错误分流决策

- **WHEN** 某候选调用抛出 `ProviderException`
- **THEN** 系统 SHALL 经 `ErrorClassifier.classify(errorType)` 得到 `FailoverDecision`
- **THEN** 决策为 `NONE` 时 SHALL 直接抛出原异常（不转移）
- **THEN** 决策为 `L1` 时 SHALL 按策略处置（FAIL_OVER 继续下一候选；FAIL_RETRY 同渠道换 Key；FAIL_FAST 抛错）

#### Scenario: L1 全耗尽抛最后异常

- **WHEN** 所有 L1 候选均失败（FAIL_OVER 策略下）
- **THEN** 系统 SHALL 抛出最后捕获的异常
- **THEN** 系统 SHALL NOT 调用任何降级服务

### Requirement: 转移事件发布

`ChannelFailoverInvoker` SHALL 在 L1 换候选前（FAIL_OVER 策略下）发布 `FailoverOccurredEvent`，记录转移事件用于容灾可观测。事件字段 SHALL 包含 `traceId`/`applicationId`/`fromChannelId`/`fromEndpointId`/`toChannelId`/`toEndpointId`/`errorType`/`decision`/`exhausted`/`occurredAt`。**删除** `fromClusterId`/`toClusterId`/`commonCauseSkip` 字段（Cluster 与共因跳过退场）。

#### Scenario: 转移事件不含 clusterId 与 commonCauseSkip

- **WHEN** 候选转移发生
- **THEN** 事件 SHALL 不包含 fromClusterId/toClusterId/commonCauseSkip 字段

## REMOVED Requirements

### Requirement: L1 共因跳过
**Reason**: Cluster 故障域根实体退场，基于 clusterId 的共因跳过机制随之移除。共因跳过误杀不共因候选（如同供应商不同账户的多 Key，账户额度独立、故障不共因，却被同 clusterId 跳过），且与 Application 在路由走向上职责交叉。故障跳过改由端点级熔断器（连续失败 OPEN 后跳过）+ 应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 控制 L0/L1）承担，不引入共因分组。
**Migration**: `ChannelFailoverInvoker` 删除 `commonCauseFailedClusters` 局部 Set 与跳过判定逻辑，L0/L1 行为改为按应用 `failureStrategy` 控制。`RoutingContext.clusterId`、`FailoverOccurredEvent`/`FailoverEvent`/`FailoverEventDo`/`FailoverEventResponse` 的 `commonCauseSkip` + `fromClusterId`/`toClusterId` 字段删除，`FailoverEventGateway.findRecent` 的 clusterId 过滤参数删除。
```

## openspec/changes/refactor-resilience-to-application-strategy/specs/resilience-console/spec.md

- Source: openspec/changes/refactor-resilience-to-application-strategy/specs/resilience-console/spec.md
- Lines: 1-55
- SHA256: 94a5fb3e45261e17309d124b13548d13f9543f1b60daed3d3654554d91241602

```md
# Resilience Console Delta Spec

## MODIFIED Requirements

### Requirement: 容灾总览页

容灾总览页 SHALL 作为容灾态势可观测大盘，展示转移事件流、耗尽告警、端点熔断状态。**删除** 故障域拓扑（Cluster 分组）与按 clusterId 归域展示（Cluster 退场）。**删除** 转移事件流的 clusterId 与「是否共因跳过」展示（共因跳过退场）。**新增** 端点熔断状态大盘区块，展示各端点熔断器当前状态（CLOSED/OPEN/HALF_OPEN）。

**展示内容**:
- 转移事件流（按 occurredAt 倒序，含 from→to 渠道、错误类型、决策、耗尽标记；**不含** clusterId/commonCauseSkip）
- 耗尽告警（exhausted=true）
- 端点熔断状态大盘（各端点熔断器状态 + 应急操作入口）

#### Scenario: 总览页展示端点熔断状态

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL 展示各端点熔断器当前状态（CLOSED/OPEN/HALF_OPEN）
- **THEN** 管理员可从大盘触发 forceOpen/forceClose 应急操作

#### Scenario: 总览页不展示 Cluster 拓扑与共因跳过

- **WHEN** 管理员访问容灾总览页
- **THEN** 页面 SHALL NOT 展示 Cluster 拓扑、按 clusterId 归域、共因跳过标记

### Requirement: 应用管理页容灾模式选择

应用管理页 SHALL 支持管理员为应用配置失败处理策略（`FAIL_FAST`/`FAIL_OVER`/`FAIL_RETRY`）与应用级超时 timeout。**移除** 容灾画像绑定相关（已随 ResilienceProfile 退场）。应用渠道 priority 排序配置（ChannelManageModal）保留。

#### Scenario: 管理员配置应用失败处理策略

- **WHEN** 管理员在应用编辑页选择失败处理策略（如 FAIL_RETRY）
- **THEN** 系统 SHALL 保存到应用 `failureStrategy` 字段
- **THEN** 该应用请求的 L0/L1 行为 SHALL 按所选策略执行

#### Scenario: 管理员配置应用超时

- **WHEN** 管理员在应用编辑页配置 timeout
- **THEN** 系统 SHALL 保存到应用 `timeout` 字段

## ADDED Requirements

### Requirement: 端点熔断应急操作

管理员 SHALL 能从前端对端点执行熔断应急操作：一键强制熔断（forceOpen，摘流量）、一键强制恢复（forceClose，解除手动熔断）、查询熔断器状态。操作入口位于 Channels 页端点维度与容灾总览页熔断状态大盘。

#### Scenario: 管理员一键熔断故障端点

- **WHEN** 管理员在端点维度点击「强制熔断」
- **THEN** 系统 SHALL 调用 forceOpen 使端点熔断器进入 OPEN
- **THEN** 该端点流量立即被切断

#### Scenario: 管理员一键恢复端点

- **WHEN** 管理员在端点维度点击「强制恢复」
- **THEN** 系统 SHALL 调用 forceClose 使端点熔断器回到 CLOSED 并重置窗口
```

