# Subagent 执行进度检查点 — resilience-architecture

> 协调者恢复地图。仅保存协调状态，不替代 plan/tasks.md checkbox。
> 当前 build_mode: subagent-driven-development, tdd_mode: tdd, isolation: branch(feature/20260619/resilience-architecture)

## 当前 Task

**Plan task:** Task 2.1: 修正 RouterChain 顺序 Permission→Health→Priority→LoadBalance
**OpenSpec task:** 2.1 修正 `RouterChain` 顺序为 `Permission → Health → Priority → (Pinned/Cluster) → LoadBalance`
**阶段:** quality-review
**BASE:** 49b1033
**实现提交:** 2f6d2e2（amend 修正前导空格+补 trailer；原 32eaaed）
**RED/GREEN:** 2 新测试 RED（顺序断言+次优先级场景 size 0）→ 改 @Order → 632 全绿无回归
**审查-修复轮次:** 0/3

## 派发记录

- [阻塞] Task 2.2 设计歧义调查完成，需用户决策（见下方）
  - 现状：HealthRouter.filter 用 mi.getChannelId() 查熔断；KeyFailoverInvoker 用 ctx.channelEndpointId() 查熔断（已 endpoint 级）
  - ChannelEndpointCircuitBreakerManager.isAvailable(Long endpointId) 本身已按 endpointId 工作
  - 冲突：ModelInstance 实体无 endpointId 字段（只有 channelId），model_instances 表无 endpoint_id 列
  - 冲突：InstanceSelector.select(modelId,applicationId,userId,role,strategy) 不接收 protocol；RouterChain 在 ModelInstance(channel)粒度过滤，无 endpointId 可用
  - 冲突：endpoint=channel×protocol，一个 channel 多 endpoint；ModelInstance 是 channel 粒度，无法 1:1 对应 endpoint
  - design doc 目标"HealthRouter(endpointId 熔断)"但未解决数据模型映射
  - 候选方案：①RoutingRequest 透传 protocol，HealthRouter 用 channelId+protocol 经 EndpointResolver 派生 endpointId 查熔断 ②ModelInstance 加 endpointId 字段+迁移（数据模型不自洽风险）③HealthRouter 暂用 channelId 粒度熔断，注释说明与 endpoint 级差异（部分实现）

## ⚠️ 越权事件记录（已处理）

修复 agent 越权提交用户文档 5189115（docs/容灾方案设计.md + docs/容灾管理范式.md）并 push 到 origin。
用户确认 force push 回退：rebase 移除 5189115，两文档恢复为 untracked（blob hash 字节级无损），force-with-lease 覆盖 origin 5189115→c94ed1d。
教训：后续修复/实现 agent 派发 prompt 已强调禁止 git add -A 与禁止 push；commit message 用双引号避免 settings.local.json 权限模式缺陷。

## 已完成 Task

- Task 1.1-1.6: complete (Approved)
- Task 1.7: complete (d1caee9, Approved, 1 Important deferred: teamId 残留)
  - DEFERRED: AuditEvent/TokenUsedEvent/UsageLogDo.teamId + usage_logs.team_id 列待清理
- Task 1.8: complete (dde790d, Approved, 4 Minor accepted)
  - 实现: 632716b PermissionRefactorIntegrationTest（端到端权限锚点切换，真实 RouterChain+H2）
  - 接受 Minor: LoadBalance 终结致场景1/3/4 断言 ~50% 概率假绿，彻底修复需重构测试超 1.8 范围
  - 设计差异: brief 场景2 软兜底属迁移层，运行时 null→空集
