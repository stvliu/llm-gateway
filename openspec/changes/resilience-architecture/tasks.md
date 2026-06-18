# Tasks

> 依赖顺序：P-r → P0 → P1 → P2（四段，原 P2/P3 已合并）。前端任务跟随后端各阶段 API 落地。
> design 阶段深化结论：砍 ApplicationModel（D8）、ADMIN 退管理面（D9）、合并 P2/P3（D10）。

## 1. P-r 权限重构（最优先，地基）

- [x] 1.1 新增 `Application` 聚合根实体 + `applications` 表（code/name/description/state + 审计字段 + 预留配额/看板字段留空）
- [ ] 1.2 新增 `ApplicationChannel` 实体 + 表（应用-渠道授权）
- [ ] 1.3 `UserApiKey` 增加 `application_id`（权限锚点），Application/ApplicationChannel Gateway 与基础设施实现
- [ ] 1.4 重写 `PermissionRouter`：过滤依据从 `UserTeam → TeamChannel` 改为 `Application → ApplicationChannel`；**移除 ADMIN 跳过分支**（数据面无特权，D9）
- [ ] 1.5 `RoutingRequest`/拦截器：权限锚点从 userId 改为 applicationId
- [ ] 1.6 数据迁移脚本：1 Team → 1 默认 Application，TeamChannel → ApplicationChannel 1:1 平移；归属不明 Key 归 `migration-default`（按原 Team 渠道集授权，非全局）；可重跑、幂等、迁移前后授权集合比对校验（D7/D9）
- [ ] 1.7 移除 `Team`/`UserTeam`/`TeamChannel` 实体及相关 Gateway/Controller/服务；废弃团队模型可见性机制（`listAllowedModels`）（D8）
- [ ] 1.8 P-r 单元与集成测试（ApplicationChannel 过滤、权限锚点切换、无 ADMIN 跳过、迁移正确性）
- [ ] 1.9 前端：Teams 页改造为 Applications 页（建/编辑应用、绑 Key、渠道授权平移）；移除成员管理 Modal；移除模型可见性 Modal（D8）；路由 `teams`→`applications`、菜单、权限常量新增 `APPLICATION_READ/WRITE`、清理 team.ts/useTeams.ts

## 2. P0 修根因（路由与熔断错配）

- [ ] 2.1 修正 `RouterChain` 顺序为 `Permission → Health → Priority → (Pinned/Cluster) → LoadBalance`
- [ ] 2.2 统一熔断 key 为 endpointId，`HealthRouter` 改用端点级熔断，与 `KeyFailoverInvoker` 共享熔断器
- [ ] 2.3 `ProviderHealthTracker` 职责收窄为供应商级粗粒度信号（仅 L2 备选模型可用性）
- [ ] 2.4 P0 单元测试（路由顺序、次优先级渠道被选、熔断 key 一致）

## 3. P1 L1 Channel 级转移回路（核心）

- [ ] 3.1 RouterChain（Permission+Health+Priority）联合产出「应用可见+健康+按 cluster/priority 排序」候选列表；LoadBalanceRouter 改返回排序列表或退场（D5/深化点5）
- [ ] 3.2 `InstanceSelector.select` 改为返回候选列表；`RoutingResolver` 适配
- [ ] 3.3 新增 `ChannelFailoverInvoker`：在候选列表内逐个试（实时查熔断跳过），按错误分流表决定 L1/L2/NONE，L0 在内部跑，L1 全耗尽才进 L2
- [ ] 3.4 错误分流表实现（INVALID_REQUEST 不转移，其余按 ProviderErrorType 映射 L1/L2/NONE）
- [ ] 3.5 流式转移边界：只在首字节前转移
- [ ] 3.6 `DegradationInvoker` 退场或降级为内部组件
- [ ] 3.7 P1 单元与集成测试（L1 转移、错误分流、流式边界、跨 Cluster 不越权、两对照场景端到端）

## 4. P2 应用级容灾画像 + Cluster 故障域分组（原 P2/P3 合并，D10）

- [ ] 4.1 新增 `ResilienceProfile` 实体 + `resilience_profiles` 表 + Gateway 实现
- [ ] 4.2 新增 `Cluster` 实体 + `clusters` 表；`Channel.cluster_id` FK（直接建实体，不经软字段，D10）
- [ ] 4.3 `Application` 挂 `resilience_profile_id`；解析链 Application → Global
- [ ] 4.4 `ResilienceResolver` 实现；预设档位（default/strict/aggressive/batch）初始化数据
- [ ] 4.5 容灾模式档位 → Profile 字段自动推导
- [ ] 4.6 会话亲和：SessionAffinityStore 接口 + Redis(生产)/InMemory(开发) 双实现；X-Session-Id→channelId，TTL 30min，亲和优先非强制（熔断则转移并更新），标识缺失不亲和（D6/深化点6）
- [ ] 4.7 Cluster 级健康聚合（域内全熔断→DOWN，任一 half-open 成功→解除）；`ClusterAffinityRouter`（就近/按域锁定）
- [ ] 4.8 `DegradationService.degrade(reason)` 按 reason 分流，L2 受画像门禁
- [ ] 4.9 `RoutingRequest` 增加 resilienceProfile 贯穿 RouterChain 与 Invoker 链；新增 PinnedModelRouter
- [ ] 4.10 P2 单元与集成测试（解析链、档位推导、会话亲和、画像继承、Cluster 健康聚合、共因隔离、亲和路由）
- [ ] 4.11 前端：画像模板页（CRUD，专家字段折叠）+ Applications 页容灾模式选择 + 降级兜底开关；容灾总览页（故障域拓扑 + 实时转移事件流 + 耗尽告警）；Channels 页一键熔断/恢复/紧切域

## 5. 收尾

- [ ] 5.1 移除 team-channel-management spec，新增 application/application-access-control/channel-failover/resilience-profile/cluster-failover/resilience-console spec
- [ ] 5.2 全链路回归测试（权限重构 + 容灾双线端到端）
- [ ] 5.3 文档更新（容灾方案设计/管理范式与实现对齐）
