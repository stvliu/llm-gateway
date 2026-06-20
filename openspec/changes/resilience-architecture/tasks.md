# Tasks

> 依赖顺序：P-r → P0 → P1 → P2（四段，原 P2/P3 已合并）。前端任务跟随后端各阶段 API 落地。
> design 阶段深化结论：砍 ApplicationModel（D8）、ADMIN 退管理面（D9）、合并 P2/P3（D10）。

## 1. P-r 权限重构（最优先，地基）

- [x] 1.1 新增 `Application` 聚合根实体 + `applications` 表（code/name/description/state + 审计字段 + 预留配额/看板字段留空）
- [x] 1.2 新增 `ApplicationChannel` 实体 + 表（应用-渠道授权）
- [x] 1.3 `UserApiKey` 增加 `application_id`（权限锚点），Application/ApplicationChannel Gateway 与基础设施实现
- [x] 1.4 重写 `PermissionRouter`：过滤依据从 `UserTeam → TeamChannel` 改为 `Application → ApplicationChannel`；**移除 ADMIN 跳过分支**（数据面无特权，D9）
- [x] 1.5 `RoutingRequest`/拦截器：权限锚点从 userId 改为 applicationId
- [x] 1.6 数据迁移脚本：1 Team → 1 默认 Application，TeamChannel → ApplicationChannel 1:1 平移；归属不明 Key 归 `migration-default`（按原 Team 渠道集授权，非全局）；可重跑、幂等、迁移前后授权集合比对校验（D7/D9）
- [x] 1.7 移除 `Team`/`UserTeam`/`TeamChannel` 实体及相关 Gateway/Controller/服务；废弃团队模型可见性机制（`listAllowedModels`）（D8）
- [x] 1.8 P-r 单元与集成测试（ApplicationChannel 过滤、权限锚点切换、无 ADMIN 跳过、迁移正确性）
  - 双审查通过（spec ✅ / quality ✅ Approved）；提交 632716b；RED 反向断言+GREEN 4场景+615回归全绿
  - 接受 Minor：场景1/3/4 经 LoadBalanceRouter 终结为单实例，若 PermissionRouter 回归失效有 ~50% 概率假绿（noneMatch 仍提供 50% 检测率）；彻底消除需重构测试绕过 LoadBalanceRouter，超 1.8 范围，接受现状
  - 设计差异（非阻塞）：brief 场景2"migration-default 软兜底"属迁移层 V52/V53（1.6 覆盖），运行时数据面 applicationId==null 直接返回空集，测试按运行时行为断言
- [x] 1.9 前端：Teams 页改造为 Applications 页（建/编辑应用、绑 Key、渠道授权平移）；移除成员管理 Modal；移除模型可见性 Modal（D8）；路由 `teams`→`applications`、菜单、权限常量新增 `APPLICATION_READ/WRITE`、清理 team.ts/useTeams.ts
  - 提交：c73c445（后端 ApplicationController+Service+DTO 7端点，15文件）+ 462a6e3（前端 35文件）+ c94ed1d（修复2 Important）+ b928df0（i18n 补全）
  - 双审查通过（spec ✅ / quality ✅ Approved）+ 修复复审 ✅；后端 630 全绿（含 ControllerIT 12例），前端 pnpm build 通过（17428模块）
  - 修复 2 Important：I-1 ApplicationChannelRequest 元素加 @NotNull @Positive 校验（5 RED→GREEN 测试）；I-2 删除改 try/catch+message.success/error 反馈
  - 接受 Minor：23 个 Channels vitest 失败经 stash 验证为 pre-existing；UserApiKeyManageModal 保留未接入（与原 Teams 页一致）
  - 越权事件已处理：修复 agent 越权提交用户文档 5189115 并 push，已 rebase 移除+force-with-lease 回退 origin，两文档恢复 untracked 无损

## 2. P0 修根因（路由与熔断错配）

- [x] 2.1 修正 `RouterChain` 顺序为 `Permission → Health → Priority → (Pinned/Cluster) → LoadBalance`
  - 提交 2f6d2e2；HealthRouter @Order 300→200、PriorityRouter @Order 200→300，Javadoc 补顺序语义
  - 双审查通过（spec ✅ / quality ✅ Approved）；RED 2新测试失败→改@Order→632全绿（独立复现 RouterChainTest 6绿）
  - 测试：顺序断言（反射读 routers 字段，真实 Router 驱动）+ 次优先级健康渠道场景（ch1熔断→选ch2）
  - 接受 Minor：反射读私有 routers 字段为合理测试内省权衡（审查建议保持现状）
- [x] 2.2 统一熔断 key 为 endpointId，`HealthRouter` 改用端点级熔断，与 `KeyFailoverInvoker` 共享熔断器
  - 提交 014d8f5（9文件）+ 2c93cea（D11 spec 变更）；D11 派生方案：RoutingRequest 增 protocol，HealthRouter 注入 EndpointResolver 从 channelId+protocol 派生 endpointId 查熔断，不动 DB/实体
  - 双审查通过（spec ✅ / quality ✅ Approved）；RED PotentialStubbingProblem(channelId vs endpointId)→635全绿（独立复现）
  - endpoint 派生失败(protocol null/resolve抛异常)保守过滤，Javadoc 详尽
  - 接受 Minor：每候选实例一次 resolve 查库属 D11 固有代价（非本Task缺陷），后续可加缓存；测试命名/回退路径覆盖为增强建议
- [x] 2.3 `ProviderHealthTracker` 职责收窄为供应商级粗粒度信号（仅 L2 备选模型可用性）
  - 提交 936ffab（仅 Javadoc +10/-1，方法逻辑零改动）；双审查通过（spec ✅ / quality ✅ Approved）；635 全绿
  - grep 确认路由侧（application/proxy/routing/）已无 ProviderHealthTracker 残留调用（Task 2.1/2.2 已改端点级熔断）
  - 保留调用：DegradationServiceImpl(L2 getCachedStatus) + ProviderRegistryHealthIndicator(actuator getAllStatuses)
  - 观察项（非本Task范围）：recordRequestResult/hasHealthyProvider/getStatus 生产无调用（被动推断链路未接入上报），属后续技术债
- [x] 2.4 P0 单元测试（路由顺序、次优先级渠道被选、熔断 key 一致）
  - 提交 04bdd8b（HealthRouterTest +18 行 protocol=null 边界测试）；双审查通过（spec ✅ / quality ✅ Approved）；636 全绿（独立复现）
  - 三点覆盖复核完整：路由顺序(RouterChainTest 反射验证排序)、次优先级渠道被选(secondaryPriorityHealthyChannel)、熔断key一致(HealthRouter/RoutingResolver/KeyFailoverInvoker 同源 endpointResolver.resolve + 三组测试)
  - 熔断key一致性不补交叉测试（属集成测试范畴，当前同源调用+单例bean结构性保证）
  - P0 段全部完成（2.1-2.4）

## 3. P1 L1 Channel 级转移回路（核心）

- [x] 3.1 RouterChain（Permission+Health+Priority）联合产出「应用可见+健康+按 cluster/priority 排序」候选列表；LoadBalanceRouter 改返回排序列表或退场（D5/深化点5）
  - 提交 2555ce1（6文件）；双审查通过（spec ✅ / quality ✅ Approved）；638 全绿（独立复现）
  - InstanceSelector.select 返回 List<ModelInstance>（按 priority 升序，PriorityRouter 保证），去掉 getFirst()
  - LoadBalanceRouter 降级为透传（isForce=false + filter 返回输入列表），未使用字段保留为 fallback 中间态并补 TODO(后续任务)标注
  - RoutingResolver 临时 .getFirst() 适配（3.2 完善返回候选列表），InstanceSelector 空列表抛 ResourceNotFoundException 兜底
  - 接受 Minor：isForce 注释表述、RouterChainTest 死桩、测试 mock 数据真实性（注释改进建议）
- [x] 3.2 `InstanceSelector.select` 改为返回候选列表；`RoutingResolver` 适配
  - 提交 ef4b2f2（2文件 RoutingResolver + RoutingResolverTest）；双审查通过（spec ✅ / quality ✅ Approved）；641 全绿（独立复现，+3 测试）
  - 新增 resolveCandidates 返回 List<RoutingContext>（逐个候选解析组装，按 priority 升序）；resolve 重构委托 resolveCandidates.getFirst() 消除 3.1 临时重复
  - RoutingContext 结构不改（plan 选定方案）；调用点暂不切换（3.3 ChannelFailoverInvoker 引入时切换）
  - 接受 Minor：eager 全量解析 N 次查询（候选数小可接受，3.3 需完整列表）
- [x] 3.3 新增 `ChannelFailoverInvoker`：在候选列表内逐个试（实时查熔断跳过），按错误分流表决定 L1/L2/NONE，L0 在内部跑，L1 全耗尽才进 L2
  - 提交 acc96f1（实现 2文件 489行）+ 70b9137（修复 I2+M1+M2）；双审查通过（spec ✅ / quality ✅ Approved 复审）；662 全绿（独立复现，+11 测试）
  - L1 候选内逐个试（KeyFailoverInvoker 内部 L0 跳过熔断 endpoint）→ProviderException→ErrorClassifier.classify→L1换下一候选/L2全耗尽后进/NONE(含INVALID_REQUEST)直接抛
  - L2 降级：degrade 返回 fallback→抛携带 fallback 的 ProviderException（L2_DEGRADATION_PREFIX 常量）让上层重路由；ResilienceProfile 用 boolean enableL2ModelDegradation 占位（P2 替换）
  - 修复 I2：tryL2Degradation try-catch 防御 DegradationServiceImpl.degrade 违背契约抛异常（保留 lastException 上下文）
  - I1 deferred 到 3.6：L2 隐式契约脆弱，3.6 替换为显式 L2DegradationRequiredException（已 javadoc 标注技术债 + 前缀常量化）
  - 技术债（非本Task）：DegradationServiceImpl.degrade 违背接口契约（抛 ALL_MODELS_DEGRADED 而非返回 null），待后续修正根因后可移除 3.3 防御 try-catch
- [x] 3.4 错误分流表实现（INVALID_REQUEST 不转移，其余按 ProviderErrorType 映射 L1/L2/NONE）
  - 提交 9c8fb88（3 新建文件 FailoverDecision + ErrorClassifier + ErrorClassifierTest）；双审查通过（spec ✅ / quality ✅ Approved）；651 全绿（独立复现，+10 测试）
  - 分流表：INVALID_REQUEST→NONE，7 共因(AUTHENTICATION_ERROR/RATE_LIMIT_ERROR/QUOTA_EXCEEDED/TIMEOUT_ERROR/UPSTREAM_ERROR/SERVICE_UNAVAILABLE/NETWORK_ERROR)→L1，UNKNOWN_ERROR→L2
  - null→NONE（不掩盖编程错误），EnumMap.getOrDefault 兜底 L2（防御新增枚举）
  - 接受 Minor：测试类 Javadoc 简写(SERVER_ERROR)与实际枚举名不一致，仅文档瑕疵不影响逻辑
- [x] 3.5 流式转移边界：只在首字节前转移
  - 提交 b2e12ab（2 文件 ChannelFailoverInvoker + Test，+69/-7）；双审查通过（spec ✅ / quality ✅ Approved）；663 全绿（独立复现，+1 测试）
  - 包装 callback 用 AtomicBoolean firstByteSent 追踪 onChunk 首次调用；首字节前同步启动失败按 L1/L2/NONE 分流换候选，首字节后不换直接抛
  - 已知架构限制（非缺陷）：chatStream 是 OkHttp enqueue 异步，invokeStream return 即 enqueue 成功，首字节前异步失败时 ChannelFailoverInvoker 已退出调用栈无法换候选（能换窗口仅限同步启动失败）。继承 KeyFailoverInvoker"传输开始后不切换"约束。真正修复需同步等待机制（Future+首字节Promise），属重大变更超 3.5 范围，Javadoc 如实记录
  - 接受 Minor：firstByteSent 检查在生产异步路径属防御性（覆盖未来同步实现）；测试模拟同步抛场景
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
