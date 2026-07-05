---
comet_change: refactor-resilience-to-application-strategy
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-05-refactor-resilience-to-application-strategy
status: final
---

# 容灾重构：删除 Cluster + 引入应用级失败处理策略

## 设计目标

1. 删除 Cluster 故障域聚合根与共因跳过，消除与 Application 的职责交叉
2. 引入轻量应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），支持下游场景差异化
3. 容灾走向由应用策略 + ApplicationChannel.priority + 端点级熔断器承担
4. 补齐管理员容灾管理前端功能

## 背景

刚归档的 simplify-resilience-architecture change 建立了 Cluster + 共因跳过。实践中发现：
- Cluster（渠道侧全局共因分组）与 Application（应用侧 priority）职责交叉，配置不可见、语义冲突
- 共因跳过误杀不共因候选（同供应商不同账户多 Key，账户额度独立不共因，却被同 clusterId 跳过）
- 共因跳过收益（首次故障省几次失败）配不上复杂度，熔断器已覆盖持续故障

下游场景对失败处理诉求差异大，需应用级策略表达。

## 核心设计

### 失败处理策略（三选一互斥）

Application 新增 `failureStrategy` 枚举字段（轻量单字段，不独立实体，不走 ResilienceProfile 回头路）：

| 策略 | L0 同渠道换 Key | L1 换渠道 | 行为 |
|------|----------------|----------|------|
| `FAIL_FAST`（快速失败） | ❌ | ❌ | 首个 Key 失败立即抛错 |
| `FAIL_OVER`（失败转移） | ✅ | ✅ | Key 用完换渠道，全耗尽抛错（当前行为） |
| `FAIL_RETRY`（失败重试，默认） | ✅ | ❌ | 同渠道换 Key，不换渠道 |

三者递进：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER（L0/L1 逐级启用）。

### 策略实现

`ChannelFailoverInvoker` 按候选所属应用的 `failureStrategy` 控制 L0/L1：
- `FAIL_FAST`：候选首个 Key 失败立即抛错，不调 KeyFailoverInvoker 换 Key、不换渠道
- `FAIL_RETRY`：调 KeyFailoverInvoker（L0 换 Key），不换渠道，同渠道 Key 耗尽抛错
- `FAIL_OVER`：L0 换 Key + L1 按 priority 换渠道，全耗尽抛错

Invoker 需获取应用 failureStrategy：经 ApplicationGateway 查询或 RoutingRequest 透传（design 阶段定，倾向 RoutingRequest 透传避免每请求查 DB）。

### 默认值与数据迁移

- 默认 `FAIL_RETRY`（契合「同供应商多 Key」主场景，K1 限流换 K2）
- 数据迁移：现有应用 `failureStrategy` 设为 `FAIL_OVER`，保持原行为（L0+L1 均跑）
- 新应用默认 `FAIL_RETRY`

### 故障跳过机制

- L1 不做共因跳过
- 端点级熔断器（ChannelEndpointCircuitBreakerManager）作为故障跳过补充：端点连续失败 → OPEN → 后续跳过
- 管理员可手动 forceOpen/forceClose 应急
- 策略与熔断器正交：策略控制候选间转移，熔断器控制端点级跳过

## 删除范围

### 后端
- Cluster 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- Channel.clusterId、RoutingContext.clusterId
- ChannelFailoverInvoker 共因跳过逻辑（commonCauseFailedClusters + 跳过判定 + publishFailoverEvent 的 commonCauseSkip）
- FailoverOccurredEvent/FailoverEvent/FailoverEventDo/FailoverEventResponse 的 commonCauseSkip + fromClusterId/toClusterId
- FailoverEventGateway.findRecent 的 clusterId 过滤参数
- FailoverEventListener.toEntity 的 commonCauseSkip 透传

### Flyway
- V65: DROP TABLE clusters
- V66: ALTER TABLE channels DROP COLUMN cluster_id
- V67: ALTER TABLE failover_events DROP COLUMN from_cluster_id, to_cluster_id, common_cause_skip
- V68: ALTER TABLE applications ADD COLUMN failure_strategy VARCHAR NOT NULL DEFAULT 'FAIL_RETRY' + 数据迁移 UPDATE applications SET failure_strategy='FAIL_OVER'

### 前端
- Cluster 拓扑卡片 + grouping.ts + 共因跳过列
- Cluster 相关 types/services/hooks

## 新增范围

### 后端
- FailureStrategy 枚举
- Application.failureStrategy 字段 + ApplicationDo/Request/Response 适配
- ChannelFailoverInvoker 按策略控制 L0/L1

### 前端
- 端点熔断应急 UI（Channels 页端点维度 forceOpen/forceClose + 状态）
- 端点熔断状态大盘（容灾总览页）
- 应用失败处理策略配置 UI（ApplicationFormModal）
- 容灾总览页重组（事件流 + 耗尽告警 + 熔断状态大盘）

## 测试策略

### 后端 TDD
- FAIL_FAST：首个 Key 失败立即抛错，不换 Key 不换渠道
- FAIL_RETRY：同渠道换 Key，不换渠道，Key 耗尽抛错
- FAIL_OVER：换 Key + 换渠道，全耗尽抛错
- 默认 FAIL_RETRY（未指定时）
- 删 Cluster 后无残留引用
- 全量回归

### 前端
- vitest + npm run build（tsc 类型检查）
- 策略配置 UI 测试
- 端点熔断应急 UI 测试
- 总览页重组测试

### 端到端
- 三策略行为 + 端点熔断应急 + priority 顺序转移 + 跨供应商转移

## 风险与取舍

- **默认 FAIL_RETRY 的取舍**：契合同供应商多 Key 主场景；渠道全失败时不自动换渠道（应用要跨渠道容灾配 FAIL_OVER）。与 FAIL_OVER 在渠道内代价相同（都试完同渠道 Key），区别只在渠道全失败后。
- **共因跳过删除代价**：跨供应商共因首次故障多试 1 次，熔断器 OPEN 后缓解。
- **BREAKING**：Cluster 端点整删、转移事件字段变更、Application 默认策略变更（现有应用迁移 FAIL_OVER）。
- **回头路边界**：应用级策略仅为单枚举字段，不演变为 ResilienceProfile（独立实体+全局解析链+L2/PinnedModel/会话亲和）。

## spec 能力影响

- New: `application-failure-strategy`（应用级失败处理策略）
- Modified: `application`（加 failureStrategy 字段）
- Modified: `channel-failover`（L1 按策略控制 L0/L1，REMOVED 共因跳过，删转移事件 clusterId/commonCauseSkip）
- Modified: `resilience-console`（总览页重组，应用管理页加策略配置，ADDED 端点熔断应急）
- 退场: `cluster-failover`（归档时删主 spec 目录）
