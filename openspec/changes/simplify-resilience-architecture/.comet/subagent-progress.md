# Subagent-Driven Development 进度检查点

> change: simplify-resilience-architecture
> plan: docs/superpowers/plans/2026-06-30-simplify-resilience-architecture.md
> branch: feature/20260630/simplify-resilience-architecture
> build_mode: subagent-driven-development | tdd_mode: tdd | review_mode: thorough | isolation: branch

## 协调状态

- 当前 plan task: Task 5 — 删除 DomainHealth 路由器（独立）
- 映射 OpenSpec tasks: 5.1-5.6
- 阶段: implementing
- Task 5 BASE commit: d8f6372
- review_mode: thorough（按批次/风险边界合并审查，每批最多 3 task 或跨模块边界；最终一次完整审查；各最多 2 轮审查-修复）
- 已通过审查阶段: 批次 A Approved
- 审查-修复轮次: 0（批次 B 进行中：Task 4 完成，待 Task 5+7）
- 批次 B（Task 4+5+7）审查: 删除类高风险，Task 4+5+7 完成后合并审查
- 待批次 B reviewer 重点核查: Task 4 顾虑 1（FailoverEventGatewayImpl:76 valueOf 还原 FailoverDecision，删 L2 后读历史 'L2' 记录抛 IllegalArgumentException——生产运行时风险，需 Task 6 处理历史值映射或数据迁移）
- 待最终审查 triage 的 Minor: (A-2)Task3 TDD RED 不纯粹 (A-4)copy default 脆弱 (A-7)InstanceSelector 每请求 DB 查询 (A-顾虑1)ChatDispatchServiceImpl protocolConverter dead code 待清理
- 待批次 A reviewer 重点核查: Task 2 顾虑 2（响应转换残留——跨协议换候选时 convertResponse 基于 primaryCtx 可能返回协议错误，design ID3 未覆盖的对称缺陷）

## 批次 A 审查结果（review_mode: thorough，第 1 轮）

- 审查范围: Task 1+2+3，BASE 2594941..HEAD 978b039，reviewer opus
- Spec: Task 1 ✅ / Task 2 ⚠️ / Task 3 ✅
- Critical: 无
- Important 1（必修）: 非流式响应转换残留——ChatDispatchServiceImpl 阶段6 convertResponse 基于 primaryCtx，主同协议+备跨协议成功时跳过转换返回错误协议响应，违反双 API 兼容铁律。reviewer 核验 RoutingResolver.buildContext 逐候选判 needsAdaptation + EndpointResolver 回退任意端点 → 路径真实可达。修复方案: 响应转换下沉 invoker（与流式 buildStreamCallback 对称），ChatDispatchServiceImpl 阶段6 删除。
- Minor: (2)Task3 TDD RED 不纯粹 (3)流式缺主同→备跨对称测试 (4)copy default 脆弱 (5)RoutingRequest Javadoc 称不可变但实为可变 LinkedHashMap (6)ChatDispatchServiceTest 两语句挤一行 (7)InstanceSelector 每请求多一次 DB 查询
- 裁定: 派发 fix subagent 修 Important 1 + Minor 3/5/6（同文件低成本）；Minor 2/4/7 记录待最终审查 triage
- 审查-修复轮次: 1/2（thorough 批次最多 2 轮）
- fix BASE: 978b039

## 派发单元（12 个 plan task）

| Task | 标题 | 状态 |
|------|------|------|
| 1 | 修复 PriorityRouter 选择器→排序器 | ✅ 批次 A 通过 ec68a15 |
| 2 | 调谐下沉 invoker，每候选独立 | ✅ 批次 A 通过 337d54c+96f4f2a（含响应转换下沉修复） |
| 3 | 应用级 ApplicationChannel.priority | ✅ 批次 A 通过 d83201e |
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

### Task 2 — 调谐下沉 invoker，每候选独立
- 状态: 实现完成 DONE_WITH_CONCERNS（待批次 A 审查）
- commit: 337d54c `fix(resilience): 调谐下沉 invoker，每候选独立 convert+tune`（8 文件 +438/-119）
- BASE: ec68a15..337d54c
- RED: 4 个新测试按预期失败（copy 返回 null、2.1 model 未调谐、2.5a 未转换 chunk）
- GREEN: 全量 `./mvnw -pl gateway-boot -am test` → Tests run: 832, Failures: 0, BUILD SUCCESS；27 个 invoker 测试全过
- 顾虑:
  1. 流式转换方向：依裁定3 把 chunk 转换下沉 invoker.buildStreamCallback，基于实际成功候选重建方向（保留 delegateCallback 会留 bug）。2.5a/2.5b 测试覆盖。✓ 可接受
  2. **响应转换残留（out of scope，待批次 A reviewer 核查）**：阶段6 convertResponse 仍基于 primaryCtx，主候选同协议+备候选跨协议成功时响应原样返回协议错误。请求侧修复使该路径暴露。
  3. copy() 用 default(throw) 非 abstract：避免强制改 4 个测试匿名实现。✓ 可接受
  4. 修改了非授权的 ChatDispatchServiceTest（必要同步，删阶段3/4 必然破坏），并移除 ChatDispatchServiceImpl 的 outboundTuner 构造参数。✓ 可接受
- 裁定: 顾虑 2 记为待核查项交批次 A reviewer，其余可接受
