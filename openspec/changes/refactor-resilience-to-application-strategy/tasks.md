# 实施任务清单

> change: refactor-resilience-to-application-strategy
> 范围：删 Cluster + 共因跳过（减法）+ 引入应用级失败处理策略（FAIL_FAST/FAIL_OVER/FAIL_RETRY）+ 补管理员容灾管理前端功能。
> 命名统一：FAIL_FAST（快速失败）/ FAIL_OVER（失败转移）/ FAIL_RETRY（失败重试），三选一互斥，默认 FAIL_RETRY。

## 1. 删除 Cluster 与共因跳过（减法，后端）

- [x] 1.1 删除 Cluster 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- [x] 1.2 删除 Channel.clusterId 字段 + ChannelDo/Repository 适配
- [x] 1.3 删除 RoutingContext.clusterId 字段 + 所有构造点适配
- [x] 1.4 删除 RoutingResolver.buildContext 的 clusterId 填充
- [x] 1.5 删除 ChannelFailoverInvoker 共因跳过逻辑（commonCauseFailedClusters + 跳过判定）
- [x] 1.6 删除 FailoverOccurredEvent/FailoverEvent/FailoverEventDo/FailoverEventResponse 的 commonCauseSkip + fromClusterId/toClusterId 字段
- [x] 1.7 删除 FailoverEventGatewayImpl.toEntity/toDataObject 相关字段透传 + findRecent 的 clusterId 过滤参数
- [x] 1.8 删除 FailoverEventListener.toEntity 的 commonCauseSkip 透传
- [x] 1.9 删除 publishFailoverEvent 的 commonCauseSkip 参数（保留 FailoverDecision.valueOf 容错）
- [x] 1.10 适配 ResilienceEventController/Service 的查询 API（删 clusterId 过滤）
- [x] 1.11 grep 确认无 Cluster/clusterId/commonCauseSkip 代码残留

## 2. 应用级失败处理策略（加法，后端）

- [x] 2.1 创建 FailureStrategy 枚举（FAIL_FAST/FAIL_OVER/FAIL_RETRY）
- [x] 2.2 Application 实体加 failureStrategy 字段 + ApplicationDo 适配
- [x] 2.3 ApplicationRequest/ApplicationResponse 加 failureStrategy 字段
- [x] 2.4 ApplicationServiceImpl create/update/toResponse 透传 failureStrategy（默认 FAIL_RETRY）
- [x] 2.5 ChannelFailoverInvoker 按应用 failureStrategy 控制 L0/L1 行为：
  - FAIL_FAST：候选首个 Key 失败立即抛错（不调 KeyFailoverInvoker 换 Key、不换渠道）
  - FAIL_RETRY：L0 跑（KeyFailoverInvoker 换 Key），L1 不跑（不换渠道），同渠道 Key 耗尽抛错
  - FAIL_OVER：L0 跑 + L1 跑（按 priority 换渠道），全耗尽抛错
- [x] 2.6 Invoker 需获取应用 failureStrategy（经 ApplicationGateway 或 RoutingRequest 透传）
- [x] 2.7 TDD：FAIL_FAST 首个 Key 失败立即抛错
- [x] 2.8 TDD：FAIL_RETRY 同渠道换 Key 不换渠道
- [x] 2.9 TDD：FAIL_OVER 换渠道全耗尽抛错
- [x] 2.10 TDD：默认策略 FAIL_RETRY（未指定时）

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

- [ ] 9.1 cluster-failover capability 主 spec 整体退场（归档时删目录）
- [ ] 9.2 更新 docs/容灾方案设计.md / docs/容灾管理范式.md：删 Cluster/共因跳过，加应用级失败处理策略，容灾由策略+priority+熔断器承担
- [ ] 9.3 grep 确认无残留：Cluster/clusterId/commonCauseSkip/ClusterHealthAggregator/ClusterAffinityRouter

## 10. 全链路回归

- [ ] 10.1 `./mvnw -pl gateway-boot -am test` 全绿
- [ ] 10.2 `cd gateway-console && npm run build` 通过
- [ ] 10.3 `npx vitest run` 通过
- [ ] 10.4 端到端验证：三策略行为 + 端点熔断应急 + priority 顺序转移 + 跨供应商转移
