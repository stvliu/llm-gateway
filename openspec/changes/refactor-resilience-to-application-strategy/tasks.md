# 实施任务清单

> change: refactor-resilience-to-application-strategy
> 本 change 含减法（删 Cluster）+ 加法（应用级策略+模板+前端），关键设计决策（共因替代方案、策略数据模型、维度筛选）在 design 阶段 brainstorming 定稿后，本任务清单可能调整。

## 1. 删除 Cluster 与共因跳过（减法，后端）

- [ ] 1.1 删除 Cluster 实体/Gateway/Impl/Controller/DTO/Repository/DO/Service
- [ ] 1.2 删除 Channel.clusterId 字段 + ChannelDo/Repository 适配
- [ ] 1.3 删除 RoutingContext.clusterId 字段 + 所有构造点适配
- [ ] 1.4 删除 RoutingResolver.buildContext 的 clusterId 填充
- [ ] 1.5 删除 ChannelFailoverInvoker 共因跳过逻辑（commonCauseFailedClusters + 跳过判定），保留 L1 按序转移
- [ ] 1.6 删除 FailoverOccurredEvent/FailoverEvent/FailoverEventDo/FailoverEventResponse 的 commonCauseSkip + fromClusterId/toClusterId 字段
- [ ] 1.7 删除 FailoverEventGatewayImpl.toEntity/toDataObject 相关字段透传 + findRecent 的 clusterId 过滤参数
- [ ] 1.8 删除 FailoverEventListener.toEntity 的 commonCauseSkip 透传
- [ ] 1.9 删除 publishFailoverEvent 的 commonCauseSkip 参数与 13 参数构造器改回（保留 FailoverDecision.valueOf 容错）
- [ ] 1.10 适配 ResilienceEventController/Service 的查询 API（删 clusterId 过滤）
- [ ] 1.11 grep 确认无 Cluster/clusterId/commonCauseSkip 代码残留

## 2. Flyway 迁移（删表/列）

- [ ] 2.1 Flyway V65：DROP TABLE clusters
- [ ] 2.2 Flyway V66：ALTER TABLE channels DROP COLUMN cluster_id
- [ ] 2.3 Flyway V67：ALTER TABLE failover_events DROP COLUMN from_cluster_id, to_cluster_id, common_cause_skip
- [ ] 2.4 确认 H2/PG 兼容（IF EXISTS），测试 profile 适配

## 3. 应用级容灾策略数据模型（加法，后端，design 定稿后细化）

- [ ] 3.1 design brainstorming 定稿：策略挂载方式（Application 内嵌字段组 vs 轻量子实体）
- [ ] 3.2 design brainstorming 定稿：共因替代方案（方向甲 providerId+开关 vs 方向乙纯熔断）
- [ ] 3.3 design brainstorming 定稿：策略维度筛选（共因开关/耗尽行为/成本控制/转移触发条件 最终集）
- [ ] 3.4 创建策略领域模型（实体/值对象，挂 Application）
- [ ] 3.5 Application 实体挂载策略字段 + ApplicationDo 适配
- [ ] 3.6 Flyway：applications 表加策略字段（或新建策略表，按 design）
- [ ] 3.7 策略 Gateway/Service（如有独立持久化）

## 4. 共因跳过基于 providerId 重实现（若 design 选方向甲）

- [ ] 4.1 ChannelFailoverInvoker 共因跳过改为基于 providerId + 应用策略开关
- [ ] 4.2 候选共因失败时，按策略开关决定是否跳过同 providerId 后续候选
- [ ] 4.3 TDD：应用开启共因跳过时跳过同 providerId 候选
- [ ] 4.4 TDD：应用关闭共因跳过时纯按 priority 顺序

## 5. 候选耗尽行为（降级兜底）

- [ ] 5.1 候选耗尽时按策略处置（抛错或降级到应用预配兜底模型）
- [ ] 5.2 TDD：耗尽行为=抛错时抛最后异常
- [ ] 5.3 TDD：耗尽行为=降级时转移到兜底模型候选（非 L2 自动降级链）

## 6. 场景模板

- [ ] 6.1 定义四场景模板推荐值（研发自动化/流程自动化/AGI/BI）
- [ ] 6.2 模板套用 API/Service
- [ ] 6.3 TDD：套用模板后策略设为推荐值

## 7. 应用策略 API 与前端配置

- [ ] 7.1 ApplicationController 策略配置端点（创建/更新/查询含策略）
- [ ] 7.2 前端 types/services：应用策略类型与 API
- [ ] 7.3 前端应用策略配置页（共因开关/耗尽行为/模板选择）
- [ ] 7.4 前端 ApplicationFormModal 集成策略配置
- [ ] 7.5 前端 locales 适配

## 8. 端点熔断应急 UI

- [ ] 8.1 前端 Channels 页端点维度：forceOpen/forceClose 按钮 + 状态展示
- [ ] 8.2 前端容灾总览页：端点熔断状态大盘区块
- [ ] 8.3 前端 types/services：熔断应急 API（已封装，接 UI）
- [ ] 8.4 前端 locales 适配

## 9. 容灾总览页重组

- [ ] 9.1 删除 Cluster 拓扑卡片 + grouping.ts
- [ ] 9.2 删除转移事件流表格的共因跳过列 + clusterId 展示
- [ ] 9.3 总览页重组：转移事件流 + 耗尽告警 + 端点熔断状态大盘
- [ ] 9.4 前端 types/services/locales 清除 Cluster 相关

## 10. 前端 Cluster 相关清除

- [ ] 10.1 删除 types/resilience.ts 中 Cluster/ClusterRequest 定义
- [ ] 10.2 删除 resilienceApi.clusters CRUD
- [ ] 10.3 删除 useClusters hook
- [ ] 10.4 grep 确认前端无 Cluster 残留

## 11. spec 同步与文档

- [ ] 11.1 cluster-failover capability 主 spec 整体退场（归档时删目录）
- [ ] 11.2 更新 docs/容灾方案设计.md / docs/容灾管理范式.md：删 Cluster、加应用级策略与场景模板
- [ ] 11.3 grep 确认无残留：Cluster/clusterId/commonCauseSkip/ClusterHealthAggregator/ClusterAffinityRouter

## 12. 全链路回归

- [ ] 12.1 `./mvnw -pl gateway-boot -am test` 全绿
- [ ] 12.2 `cd gateway-console && npm run build` 通过
- [ ] 12.3 `npx vitest run` 通过
- [ ] 12.4 端到端验证：四场景策略配置 + 共因跳过（开/关）+ 耗尽行为 + 熔断应急 + 跨供应商转移
