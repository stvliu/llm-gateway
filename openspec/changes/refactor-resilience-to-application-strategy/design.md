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
- 删除 Cluster 故障域聚合根与共因跳过，消除与 Application 的职责交叉
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
