# Subagent-Driven Development 进度检查点

> change: simplify-resilience-architecture
> plan: docs/superpowers/plans/2026-06-30-simplify-resilience-architecture.md
> branch: feature/20260630/simplify-resilience-architecture
> build_mode: subagent-driven-development | tdd_mode: tdd | review_mode: thorough | isolation: branch

## 协调状态

- 当前 plan task: Task 2 — 调谐下沉 invoker，每候选独立（已存缺陷，L1 前置）
- 映射 OpenSpec tasks: 2.1-2.7
- 阶段: implementing
- Task 2 BASE commit: ec68a151fbe65c15a0ac79a6424489946a4423c3
- review_mode: thorough（按批次/风险边界合并审查，每批最多 3 task 或跨模块边界；最终一次完整审查；各最多 2 轮审查-修复）
- 已通过审查阶段: 无（Task 1 待批次 A 审查）
- 审查-修复轮次: 0
- 批次 A（Task 1+2+3）审查: 待 Task 3 完成后派发

## 派发单元（12 个 plan task）

| Task | 标题 | 状态 |
|------|------|------|
| 1 | 修复 PriorityRouter 选择器→排序器 | 实现完成 ec68a15（待批次 A 审查） |
| 2 | 调谐下沉 invoker，每候选独立 | 进行中 |
| 3 | 应用级 ApplicationChannel.priority | 待派发 |
| 4 | 删除 L2 模型降级层 | 待派发 |
| 5 | 删除 DomainHealth 路由器 | 待派发 |
| 6 | Cluster 语义改造 + 瘦身字段 | 待派发 |
| 7 | 删除 PinnedModel 与会话亲和 | 待派发 |
| 8 | ResilienceProfile 实体降级 | 待派发 |
| 9 | L1 clusterId 共因跳过 | 待派发 |
| 10 | 前端适配 | 待派发 |
| 11 | spec 同步与文档 | 待派发 |
| 12 | 全链路回归 | 待派发 |

## 批次审查计划（thorough）

- 批次 A: Task 1+2+3（L1 正确性修复 + 应用级 priority，路由核心）→ 合并 spec+quality review
- 批次 B: Task 4+5+7（删除 L2/DomainHealth/PinnedModel/会话亲和，删除类高风险）→ 合并 review
- 批次 C: Task 6+8+9（Cluster 改造 + ResilienceProfile 降级 + 共因跳过，架构核心）→ 合并 review
- 批次 D: Task 10+11（前端 + spec 同步）→ 合并 review
- Task 12（全链路回归）→ 验证
- 最终: 全分支完整 review

## 实现记录

### Task 1 — 修复 PriorityRouter 选择器→排序器
- 状态: 实现完成 DONE_WITH_CONCERNS（待批次 A 审查）
- commit: ec68a15 `fix(resilience): PriorityRouter 改为排序器，主备 priority 不丢备`
- 变更文件: PriorityRouter.java、PriorityRouterTest.java、RouterChainTest.java（3 files +67/-17）
- BASE: 2594941..ec68a15
- RED: 4 个测试按预期失败（收敛缺陷，size 各少 1）
- GREEN: 全量 `./mvnw -pl gateway-boot -am test` BUILD SUCCESS，PriorityRouterTest 6/0/0、RouterChainTest 7/0/0，全 surefire 无失败
- 顾虑: (1) 定向组合命令未在 settings 授权，改用全量回归（更严格，可接受）；(2) Task 3 切换应用级映射时需回归本任务测试（已 Javadoc 标注）
- 裁定: 顾虑为观察性，不阻塞，进入批次 A 审查队列
