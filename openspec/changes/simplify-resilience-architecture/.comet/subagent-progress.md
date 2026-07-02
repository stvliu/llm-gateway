# Subagent-Driven Development 进度检查点

> change: simplify-resilience-architecture
> plan: docs/superpowers/plans/2026-06-30-simplify-resilience-architecture.md
> branch: feature/20260630/simplify-resilience-architecture
> build_mode: subagent-driven-development | tdd_mode: tdd | review_mode: thorough | isolation: branch

## 协调状态

- 当前 plan task: Task 9 — L1 clusterId 共因跳过（依赖 Task 1+6，均已完成）
- Task 9 BASE commit: e22ba3de（HEAD）
- Task 10 实现提交: e35ba30e（25 files 7删18改，build + vitest 117 tests pass）。2 项后端 gap 待用户决策:
  - gap1（Task 9 遗漏）✅ 已修复 635798a6: FailoverEventResponse 加 commonCauseSkip + toResponse L70 透传 + decision 注释改 L1/NONE + 测试，回归 BUILD SUCCESS
  - gap2（Task 3 遗漏 + plan 缺任务）: spec application-access-control L37 + resilience-console L45 要求前端保存 ApplicationChannel.priority，但后端 PUT /applications/{id}/channels 仅接 channelIds 无 priority，listChannels 返回 number[] 无 priority。Task 3 plan 3.1-3.9 只做运行时注入无保存端点。plan 与 spec 有 gap。用户决策本 change 补后端端点。探查发现后端 Gateway 层已支持 priority 读写（findByApplicationId 返回含 priority List、saveAll 接 List<ApplicationChannel> 含 priority），只缺 Controller/Service/DTO/前端 暴露层
  - gap2（Task 3 遗漏 + plan 缺任务）: spec application-access-control L37 + resilience-console L45 要求前端保存 ApplicationChannel.priority，但后端 PUT /applications/{id}/channels 仅接 channelIds 无 priority，listChannels 返回 number[] 无 priority。Task 3 plan 3.1-3.9 只做运行时注入无保存端点。plan 与 spec 有 gap
- 映射 OpenSpec tasks: 6.1-6.10
- 批次 D 审查第 1 轮: NEEDS_FIX。Important I-1（switchCluster 后端残留：接口+Impl L68+Controller PUT /channels/{id}/cluster+SwitchClusterRequest DTO+5 测试，spec resilience-console L54-56 REMOVED 紧切域但实现未跟上，clusterGateway 仅 switchCluster 用可一并删）。Minor M-1（ProviderHealthTracker L16/56 真实 {@link} 断链 DegradationServiceImpl + 过时 L2 描述，实际消费者 ProviderRegistryHealthIndicator）、M-4（ChannelManageModal handleSelectAll 全选丢已配 priority）。已派发修复 agent 第 2 轮处理 I-1+M-1+M-4
- Task 11 提交 961b8f64: delta specs 全一致 + 设计文档重写 + grep 无代码逻辑残留。发现待审查项: ChannelEmergencyService.switchCluster 后端残留（接口+Impl L68+Controller PUT /channels/{id}/cluster+3 测试，前端已删 SwitchClusterButton 但后端死端点未删，属 Task 5/7 清理遗漏）；3 处 stale Javadoc {@link} 断链（ProviderHealthTracker L16/56 指 DegradationServiceImpl、ChannelEmergencyServiceImpl L29 指 ClusterHealthAggregator、ResilienceEventServiceImpl L25 指 ResilienceProfileServiceImpl）
- Task 6 BASE commit: 4d4c7a4；实现提交 e7ecf9d（amend 含 orphan 删除）
- 字段澄清（spec cluster-failover/spec.md L17-33 权威）: Cluster 保留 code/name/providerId + 新增 description + 审计；删 region/priority/healthStatus。plan 6.1 漏写 providerId，spec L30「Cluster 与 providerId 共存正交」补全
- Task 6 实现摘要: Cluster 瘦身+description 新增；FailoverOccurredEvent/FailoverEvent/DO 加 commonCauseSkip；FailoverEventGatewayImpl L75/L76 valueOf 容错（L2/未知→NONE/null+warn）；V60 删列+加 description 列，V64 加 common_cause_skip 列
- Task 6 顾虑: implementer 环境无 mvn 权限，RED/GREEN 由协调者执行回归验证；FailoverOccurredEvent 新增 12 参数次级构造器（commonCauseSkip=false）避免触碰 ChannelFailoverInvoker（plan 6.6 保留），Task 9 改 13 参数规范构造器时可移除；FailoverEventListener.toEntity 未透传 commonCauseSkip（不在允许范围，默认 false 语义一致，Task 9 接入）
- Task 8 实现提交: 90584fb（39 files +151/-2335，删 19 类含 13 主码+6 测试，回归后台 bgj2qrhpp）
- Task 8 范围决策（用户已确认）:
  - V63（删 model_instances.priority）: **拆为独立后续任务**（Task 8 不做 V63）。ModelInstance.priority 仍活跃，删除连带 ~13 supply 域文件。本 change 结束后单独开 change 处理 supply 域 priority 清理。plan 8.2 的 V63 标记为不在本任务范围
  - timeout 运行时接入: **本轮接入运行时**（spec 合规修复）。派发修复 agent acf6111797c038ec3：RoutingResolver 注入 ApplicationGateway，resolveCandidates 查 Application.timeout，buildContext 中 `applicationTimeout!=0 ? applicationTimeout : channel.getTimeout()`。RoutingContext 不改。修复轮完成后 Task 8 进入待批次 C 审查
- review_mode: thorough（按批次/风险边界合并审查，每批最多 3 task 或跨模块边界；最终一次完整审查；各最多 2 轮审查-修复）
- 已通过审查阶段: 批次 A Approved + 批次 B Approved
- 批次 C 审查第 1 轮: NEEDS_FIX。Critical C1（FailoverEventListener.toEntity 漏透传 commonCauseSkip，DB 恒 false，spec L119 偏离）+ Important I1（FailoverOccurredEvent.java:34/FailoverEvent.java:33 Javadoc 仍提 ChannelGateway 反查）。已派发修复 agent，第 2 轮
- 批次 C（Task 6+8+9）审查: Task 6+8+9 完成后合并审查
- **Task 6 强制项（reviewer 要求）**: FailoverEventGatewayImpl.java:76 valueOf 还原 FailoverDecision 需容错——读历史 decision='L2' 行会抛 IllegalArgumentException 破坏管理后台容灾查询。Task 6 触及 FailoverEvent/DO，必须加 try-catch 容错（L2/未知值→NONE + log warn）或配套数据迁移 UPDATE failover_events SET decision='NONE' WHERE decision='L2'。不可遗漏。
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
| 4 | 删除 L2 模型降级层 | ✅ 批次 B 通过 d8f6372 |
| 5 | 删除 DomainHealth 路由器 | ✅ 批次 B 通过 a1f387b |
| 6 | Cluster 语义改造 + 瘦身字段 | 实现完成 e7ecf9d（待回归验证 + 批次 C 审查） |
| 7 | 删除 PinnedModel 与会话亲和 | ✅ 批次 B 通过 a8cefaf |
| 8 | ResilienceProfile 实体降级 | ✅ 实现完成 90584fb + timeout 接入 e22ba3de（736 tests pass，待批次 C 审查） |
| 9 | L1 clusterId 共因跳过 | ✅ 实现完成 c1f43615（744 tests pass 自报，协调者独立回归验证中 bnl2skzdq，待批次 C 审查） |
| 10 | 前端适配 + gap1 + gap2 | ✅ e35ba30e（前端适配）+ 635798a6（gap1 Response 透传）+ 50446d91（gap2 priority 保存端点+前端配置）。后端 746 tests + 前端 build + vitest 117 pass，待批次 D 审查 |
| 11 | spec 同步与文档 | ✅ 实现完成 961b8f64（delta specs 一致 + 设计文档重写，待批次 D 审查） |
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
