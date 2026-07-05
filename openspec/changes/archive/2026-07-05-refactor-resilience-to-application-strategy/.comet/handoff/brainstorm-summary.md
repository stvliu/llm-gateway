# Brainstorm Summary

- Change: refactor-resilience-to-application-strategy
- Date: 2026-07-02

## 确认的技术方案

**删 Cluster + 共因跳过，引入轻量应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY 三选一），容灾由策略+priority+熔断器承担。**

### 命名统一
- `FAIL_FAST`（快速失败）
- `FAIL_OVER`（失败转移）
- `FAIL_RETRY`（失败重试）
- 三选一互斥，默认 `FAIL_RETRY`

### 策略行为（L0=同渠道换Key，L1=换渠道）
| 策略 | L0 | L1 | 行为 |
|------|----|----|------|
| FAIL_FAST | ❌ | ❌ | 首个 Key 失败立即抛错 |
| FAIL_OVER | ✅ | ✅ | Key 用完换渠道，全耗尽抛错（当前行为） |
| FAIL_RETRY（默认） | ✅ | ❌ | 同渠道换 Key，不换渠道 |

- 三者递进：FAIL_FAST ⊂ FAIL_RETRY ⊂ FAIL_OVER
- ChannelFailoverInvoker 按应用 failureStrategy 控制 L0/L1

### 核心决策
- D1: 容灾配置收敛到 Application
- D2: 不做共因跳过（避免误杀不共因候选，熔断器兜底持续故障）
- D3: 应用级失败处理策略（Application 单枚举字段，轻量不走 ResilienceProfile 回头路）
- D4: 数据迁移——现有应用设为 FAIL_OVER 保持原行为，新应用默认 FAIL_RETRY
- D5: 端点熔断器作为故障跳过补充机制（与策略正交）
- D6: 端点熔断管理前端（forceOpen/forceClose + 状态大盘）
- D7: 容灾总览页重组（删 Cluster 拓扑 + 共因跳过列，加熔断状态大盘）

## 关键取舍与风险

- **默认 FAIL_RETRY 的取舍**：契合「同供应商多 Key」主场景（K1 限流换 K2），渠道全失败时不自动换渠道（应用要跨渠道容灾配 FAIL_OVER）。与 FAIL_OVER 在渠道内代价相同（都试完同渠道 Key），区别只在渠道全失败后。
- **共因跳过删除代价**：跨供应商共因首次故障多试 1 次，熔断器 OPEN 后缓解。
- **BREAKING**：Cluster 端点整删、转移事件字段变更、Application 默认策略变更（现有应用迁移 FAIL_OVER）。
- **回头路边界**：应用级策略仅为单枚举字段，不演变为 ResilienceProfile。

## 测试策略

- 后端 TDD：三策略行为测试（FAIL_FAST/FAIL_RETRY/FAIL_OVER 各 L0/L1 表现）+ 默认 FAIL_RETRY + 删 Cluster 后无残留 + 全量回归
- 前端：vitest + build + 策略配置 UI 测试 + 熔断应急 UI 测试 + 总览页重组测试
- 端到端：三策略行为 + 端点熔断应急 + priority 顺序转移 + 跨供应商转移

## Spec Patch（已回写）

- New: `application-failure-strategy` capability（应用级失败处理策略）
- Modified: `application`（加 failureStrategy 字段）
- Modified: `channel-failover`（L1 按策略控制 L0/L1，REMOVED 共因跳过，删转移事件 clusterId/commonCauseSkip）
- Modified: `resilience-console`（总览页重组，应用管理页加策略配置，ADDED 端点熔断应急）

## 探讨过程关键决策

- 多对多不必要、下游选分组不做
- Cluster 与 Application 职责重叠 → 删 Cluster
- 共因替代：providerId 因跨供应商共因失效；管理员配置跳过策略=共因分组=修正Cluster，与删Cluster矛盾；选全删不跳过
- 场景差异化：经四场景验证，引入轻量应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY）覆盖，不做场景模板（YAGNI）
- 默认策略：探讨快速失败/失败转移/失败重试，定 FAIL_RETRY（契合同供应商多Key主场景），现有应用迁移 FAIL_OVER
- 策略与 L0：FAIL_FAST 不跑 L0，FAIL_RETRY/FAIL_OVER 跑 L0；FAIL_RETRY 不跑 L1，FAIL_OVER 跑 L1
