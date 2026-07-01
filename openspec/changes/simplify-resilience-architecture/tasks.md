# Tasks

> 实施顺序遵循 design.md 的依赖关系：先修两个已存缺陷（L1 正确性前提），再应用级 priority，再删 L2/DomainHealth/PinnedModel/会话亲和，再 ResilienceProfile 降级，最后 Cluster 改造与共因跳过。
> 每个任务 TDD：先写失败测试，再实现，再跑绿，再 commit。

## 1. 修复 PriorityRouter 选择器→排序器（已存缺陷，L1 前置）

- [x] 1.1 grep 确认 `ModelInstance.priority` 的所有用途（负载均衡/监控/前端展示），记录是否可删
- [x] 1.2 写失败测试：主备 priority 不同时，PriorityRouter 输出完整列表 [主,备] 不丢备（补当前单元测试绕过路由链的缺口）
- [x] 1.3 写失败测试：RouterChain 经 PriorityRouter 后候选列表含全部 priority 组
- [x] 1.4 改 `PriorityRouter.filter` 为排序器：按 priority 升序输出完整列表，不收敛；调整 `isForce` 语义
- [x] 1.5 跑绿 + 回归 `./mvnw -pl gateway-boot -am test`
- [x] 1.6 commit

## 2. 修复调谐下沉 invoker，每候选独立（已存缺陷，L1 前置）

- [x] 2.1 写失败测试：候选不同 upstreamModelName 时，L1 换渠道后请求 model 正确（当前只对首项调谐导致换渠道后 model 错）
- [x] 2.2 `ChannelFailoverInvoker` 注入 `OutboundTuner`，invoke/invokeStream 内每候选试之前基于原始 request 派生副本调谐
- [x] 2.3 `ChatDispatchServiceImpl` 删阶段4对外调谐，改为传原始 request 给 invoker
- [x] 2.4 评估 `convertRequest`（跨协议转换）是否需每候选独立，按需改造
- [x] 2.5 跑绿 + 回归
- [x] 2.6 commit

## 3. 应用级 ApplicationChannel.priority 取代全局（依赖 1）

- [x] 3.1 `ApplicationChannel` 实体加 `priority` 字段 + Getter/Setter
- [x] 3.2 Flyway 迁移：`application_channels` 加 `priority` 列
- [x] 3.3 `ApplicationChannelGateway`/Impl/DO/Repository 适配 priority
- [x] 3.4 `PermissionRouter` 过滤时把 ApplicationChannel.priority 附着到候选（或 RoutingRequest 携带映射）——定注入点
- [x] 3.5 `PriorityRouter` 排序键改 ApplicationChannel.priority（无则回退默认）
- [x] 3.6 `InstanceSelector.findActiveByModelIdOrderByPriority` 适配应用级排序
- [x] 3.7 写测试：同渠道对不同应用不同 priority，各自转移顺序独立
- [x] 3.8 跑绿 + 回归
- [x] 3.9 commit

## 4. 删除 L2 模型降级层（独立）

- [x] 4.1 整删 `application/degradation/` 包（DegradationService/Impl/Properties/Event/RecoveredEvent + `@Scheduled recoveryCheck`）
- [x] 4.2 整删 `L2DegradationRequiredException`
- [x] 4.3 `ChannelFailoverInvoker` 删 `tryL2Degradation`/`degradationService` 字段/构造参数，候选耗尽直接抛 lastException，签名去 profile
- [x] 4.4 `ChatDispatchServiceImpl` 删 `invokeWithL2Failover`/`invokeStreamWithL2Failover`/`resolveMaxDepth`/`unwrapL2Cause`/`MAX_DEGRADATION_DEPTH`/`resolveProfileSafely`/`resilienceResolver` 依赖，直接调 invoker
- [x] 4.5 `FailoverDecision` 删 L2 枚举值
- [x] 4.6 `ErrorClassifier` UNKNOWN→NONE，getOrDefault 兜底改 NONE
- [x] 4.7 删 `DegradationServiceTest`，适配 `ChannelFailoverInvokerTest`/`ChatDispatchServiceTest`/`ChannelFailoverIntegrationTest`
- [x] 4.8 跑绿 + 回归
- [x] 4.9 commit

## 5. 删除 DomainHealth 路由器（独立，与 6 无强依赖）

- [x] 5.1 整删 `ClusterHealthAggregator`
- [x] 5.2 整删 `ClusterAffinityRouter`，RouterChain 顺序变为 Permission→EndpointHealth→Priority→LoadBalance
- [x] 5.3 删 `ClusterHealthStatus` 枚举（若 Cluster 不再用）
- [x] 5.4 适配 `RouterChainTest`/`HealthRouterTest`
- [x] 5.5 跑绿 + 回归
- [x] 5.6 commit

## 6. Cluster 语义改造 + 瘦身字段（独立，保留实体名）

- [x] 6.1 `Cluster` 实体删 region/priority/healthStatus 字段，保留 code/name/description + 审计；更新 Javadoc 语义为「跨供应商故障独立性分组」
- [x] 6.2 `Channel.clusterId` 字段名保留不变
- [x] 6.3 `ClusterGateway`/Impl/DO/Repository 删 region/priority/healthStatus 适配
- [x] 6.4 Flyway 迁移：clusters 表删 region/priority/health_status 列
- [x] 6.5 `ClusterController` 适配（字段瘦身，API 路径 `/api/v1/resilience/clusters` 保留）
- [x] 6.6 `ChannelFailoverInvoker.publishFailoverEvent` 的 clusterId 反查逻辑保留（语义不变）
- [x] 6.7 `FailoverOccurredEvent`/`FailoverEvent`/DO 的 clusterId 字段保留，新增 `commonCauseSkip` 标记
- [x] 6.8 适配所有 Cluster 引用测试（删 region/priority/healthStatus 相关断言）
- [x] 6.9 跑绿 + 回归
- [x] 6.10 commit

## 7. 删除 PinnedModel 与会话亲和（独立）

- [x] 7.1 整删 `PinnedModelRouter`
- [x] 7.2 删 `ResilienceProfile.enablePinnedModel`/`pinnedModelId`（若 8 未先删实体）
- [x] 7.3 整删 `SessionAffinityStore`（Redis/InMemory 双实现）+ `SessionAffinityConfig`
- [x] 7.4 删 `ResilienceProfile.enableSessionAffinity`/`sessionAffinityTtlMinutes`
- [x] 7.5 RouterChain 去除 PinnedModel（已在 5.2 处理，此处确认）
- [x] 7.6 适配测试
- [x] 7.7 跑绿 + 回归
- [x] 7.8 commit

## 8. ResilienceProfile 实体降级（依赖 4/7）

- [x] 8.1 `Application` 实体加 `timeout` 字段，删 `resilienceProfileId`
- [x] 8.2 Flyway：applications 表加 timeout 列、删 resilience_profile_id 列、删 resilience_profiles 表、删 V56 seed
- [x] 8.3 整删 `ResilienceProfile` 实体、`ResilienceProfileGateway`/Impl、`ResilienceResolver`、`ResilienceProfileApplier`
- [x] 8.4 整删 `ResilienceProfileController` + DTO
- [x] 8.5 `ChatDispatchServiceImpl`/`InstanceSelector` 的 profile 解析改为直接读 Application.timeout
- [x] 8.6 `ApplicationServiceImpl`/Controller 适配 timeout CRUD，移除 `/applications/{id}/resilience` 端点
- [x] 8.7 适配 `ResilienceProfileIntegrationTest` 等测试（重写或删除两对照场景）
- [x] 8.8 跑绿 + 回归
- [x] 8.9 commit

## 9. L1 clusterId 共因跳过（依赖 1+6）

- [x] 9.1 写失败测试：同 clusterId 共因失败时，L1 跳过同域候选试异域
- [x] 9.2 写测试：共因跳过标记仅本次请求有效，下次请求不继承
- [x] 9.3 写测试：非共因失败（NONE）不触发共因跳过
- [x] 9.4 `ChannelFailoverInvoker` invoke/invokeStream 内实现共因跳过逻辑：当前候选共因失败→标记 clusterId→跳过同域后续候选→试异域
- [x] 9.5 `publishFailoverEvent` 新增「是否共因跳过」标记
- [x] 9.6 端到端集成测试：故障域级共因故障→L1 跳过同域→跨域转移成功
- [x] 9.7 跑绿 + 回归
- [x] 9.8 commit

## 10. 前端适配（gateway-console）

- [ ] 10.1 容灾总览页：clusterId 字段保留（语义随 Cluster 改造），新增共因跳过展示，移除降级/会话亲和/PinnedModel
- [ ] 10.2 画像模板页整删（随 ResilienceProfile 退场）
- [ ] 10.3 Applications 页：移除容灾画像绑定，加 timeout 配置 + 渠道 priority 排序
- [ ] 10.4 Channels 页：clusters→clusters，移除「紧切域」（依赖已删域级路由）
- [ ] 10.5 types/services/locales 适配（Cluster 字段瘦身：删 region/priority/healthStatus，删 L2/Pinned/会话亲和相关）
- [ ] 10.6 `cd gateway-console && npm run build` 通过
- [ ] 10.7 commit

## 11. spec 同步与文档

- [ ] 11.1 确认 delta specs 与实现一致（本 change 已创建 10 个 delta specs）
- [ ] 11.2 更新 `doc/容灾方案设计.md` / `doc/容灾管理范式.md`：四层→三层，Cluster 语义改造+瘦身字段，删 L2/DomainHealth/PinnedModel/会话亲和
- [ ] 11.3 grep 确认无残留：L2/DegradationService/PinnedModel/SessionAffinity/ResilienceProfile/ClusterHealthAggregator/ClusterAffinityRouter/ModelInstance.priority 相关引用清除
- [ ] 11.4 commit

## 12. 全链路回归

- [ ] 12.1 `./mvnw -pl gateway-boot -am test` 全绿
- [ ] 12.2 `cd gateway-console && npm run build` 通过
- [ ] 12.3 端到端验证：主备 priority L1 转移、共因跳过跨域转移、调谐每候选独立、INVALID_REQUEST 不转移
- [ ] 12.4 commit（如有修复）
