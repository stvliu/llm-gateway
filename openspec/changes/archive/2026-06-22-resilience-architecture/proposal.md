## Why

当前网关存在两类核心缺口：

**容灾缺口**：只有 Key 级转移（同渠道换 Key）和模型级降级（换模型）两层，**缺失 Channel 级运行时失败转移**——同模型多渠道间无法运行时按优先级自动切换，次优先级渠道永不参与转移；模型级降级对共因故障（认证/配额/账号限流/网络/宕机）无效却被当主容灾手段；熔断 key 路由侧（channelId）与调用侧（endpointId）不一致，健康过滤与实际熔断互不可见。

**权限模型缺口**：网关作为「应用聚合分发层」，真正的调用方是应用（Claude Code/OpenClaw/ERP），但现有权限模型以「人/团队」为授权对象（`User → Team → TeamChannel`），无法干净表达「不同应用访问不同渠道与模型」——不同应用要不同渠道只能靠为每个应用建 Team 迁就，污染组织模型。授权对象应从「人/团队」转为「应用」。

本 change 双线推进：将 Application 升级为权限+行为根实体，移除 Team，使「应用」成为渠道/模型可见性与容灾画像的统一决策者；并建立四层容灾栈（Key→Channel→模型→兜底）补齐 Channel 级透明容灾。

**执行顺序（P-r 优先，容灾整体后置）**：权限重构 P-r 是地基（权限锚点、Application 实体、访问控制），最优先；容灾 P0/P1 紧随（共享终态 PermissionRouter，不重写两遍）；P2（画像+Cluster）最后在 Application 权限上落地。理由：权限模型是正确性问题（现有「不同应用访问不同渠道」要靠为每个应用建 Team 迁就，是模型错位），正确性优先于容灾可用性；且 PermissionRouter 先建终态，容灾候选来源无需从 Team 切到 Application 造成回归；Team 数据迁移宜早不宜晚。

设计依据见 `doc/容灾方案设计.md` 与 `doc/容灾管理范式.md`。

## What Changes

### P0 修根因（路由与熔断错配）—— 容灾线，P-r 之后
- 修正 `RouterChain` 顺序为 `Permission → Health → Priority → (Cluster/Pinned) → LoadBalance`：先过滤熔断渠道，再在存活渠道里按 priority 分组，使次优先级健康渠道能成为转移候选。**BREAKING**：现有「Priority 先于 Health 导致次优先级渠道永不被选」的行为将改变。
- 统一熔断 key 为 `endpointId`，`HealthRouter` 改用端点级熔断，与 `KeyFailoverInvoker` 对齐，路由侧与调用侧共享同一熔断器实例。
- `ProviderHealthTracker` 降级为供应商级粗粒度信号（仅用于 L2 备选模型可用性），不再驱动 L1 路由决策。

### P1 L1 Channel 级转移回路（核心）—— 容灾线，P-r 之后
- 新增 `ChannelFailoverInvoker`，替代现有 `DegradationInvoker` 作为运行时转移回路：按错误类型分流（L1 换渠道 / L2 换模型 / NONE 直接抛），L0（Key 级）在其内部跑，L1 全耗尽才进 L2。
- `InstanceSelector.select` 由「返回单个实例」改为「返回按 (cluster, priority) 排序的候选列表」，供 L1 逐个尝试。
- 引入错误分流表：`INVALID_REQUEST` 绝不转移（请求级错误换哪都无效），其余按 `ProviderErrorType` 映射到 L1/L2/NONE。
- 流式边界：只在首字节前转移，首字节后失败不换渠道（继承现有约束）。
- P0/P1 在 P-r 完成的 Application 权限上落地（PermissionRouter 已是终态，无需从 Team 切换数据源）；候选列表来源（权限过滤）随 P-r 完成即为 ApplicationChannel，转移逻辑本身不依赖权限模型。

### P-r 权限重构（应用为中心，移除 Team）—— 权限线，最优先
- 新增 `Application` 根实体实体 + `applications` 表，作为「权限 + 行为」双根实体：承载 N 把 Key 的应用归属、渠道可见性、模型可见性、容灾画像。
- `UserApiKey` 增加 `application_id`，权限锚点从 `userId`（人）改为 `apiKeyId` → `applicationId`（应用）。
- 渠道可见性归 Application：新增 `ApplicationChannel`（应用能访问哪些渠道）。**模型可见性不独立配置**——由渠道上挂哪些 ModelInstance 隐式决定，废弃现有「团队模型可见性」机制。权限链重写为 `UserApiKey → Application → ApplicationChannel → Channel`。
- **移除 Team 体系**：删除 `Team`、`UserTeam`、`TeamChannel` 实体及相关 Gateway；`team-channel-management` capability 作废。现有 Team/TeamChannel 数据 1:1 平移到 Application/ApplicationChannel（授权不丢），归属不明 Key 归 `migration-default` 应用。
- `PermissionRouter` 重写：过滤依据从 `UserTeam → TeamChannel` 改为 `UserApiKey.application_id → ApplicationChannel`；**移除 ADMIN 跳过分支**（ADMIN 退回管理面特权，数据面无跳过）。
- Application 预留配额/预算、用量看板字段（留空，待 `quota`/`audit` 域填充）；密钥轮换不建。

### P2 应用级容灾画像 + Cluster 故障域分组（原 P2/P3 合并）—— 容灾线，等权限重构
- 新增 `ResilienceProfile` 实体 + `resilience_profiles` 表，承载四层栈开关、错误分流覆盖、模型锁定、会话亲和、成本/超时策略。**纯数据库方案**：画像全部落库 + CRUD API + 控制台管理，预设档位（default/strict/aggressive/batch）由初始化数据写入，满足「全实体可审计」铁律。
- `Application` 挂 `resilience_profile_id` 承载画像。解析链 `Application → Global`（Team 已移除，无中间层；Application 画像为主，全局 `default` 兜底）。
- `ResilienceProfile` 暴露给管理员两个面向字段：容灾模式档位（STANDARD/STRICT/AGGRESSIVE，BATCH 为 STANDARD 的 QUEUED 变体）+ 降级兜底开关，其余专家字段由档位自动推导。
- 会话亲和：场景1（agent 多轮）按请求头 `X-Session-Id` 亲和到首次命中渠道，标识缺失时不亲和（安全降级）。SessionAffinityStore 接口 + Redis(生产)/InMemory(开发) 双实现，TTL 30min，亲和优先非强制（熔断则转移并更新）。
- 修正 `DegradationService.degrade(reason)` 语义：按 reason 分流，定位为 L2 兜底（应用可选启用），非全局主路径。
- 新增显式 `Cluster` 实体 + `clusters` 表（code/name/provider_id/region/priority/health_status），`Channel.cluster_id` FK（直接建实体，不经软字段阶段）。Cluster 级健康聚合：域内所有渠道熔断 → Cluster DOWN，路由跳过整域；域内任一渠道 half-open 成功 → Cluster 解除 DOWN。Cluster 既是故障域（共因隔离）也是亲和域（就近低延迟 / 按供应商分域锁定模型）。

### 控制台管理范式
- **屏1 容灾总览**（Dashboard 增强，只读）：故障域拓扑 + 实时转移事件流 + 耗尽告警。
- **屏2 画像模板**：模板 CRUD，专家字段折叠在「高级」，管理员克隆微调而非从零填。
- **屏3 应用管理**：新增 Applications 管理页（建应用、绑 Key、配渠道/模型可见性、选容灾模式档位）；Channels 页加一键手动熔断/恢复/紧切域应急操作（复用熔断器，无新状态机）；移除原 Teams 管理页。

## Capabilities

### New Capabilities
- `application`: 应用根实体——权限+行为双聚合，承载 Key 归属、渠道/模型可见性、容灾画像，预留配额/看板字段（本 change 启用权限+画像，配额/看板留空）。
- `application-access-control`: 应用级访问控制——`ApplicationChannel` 渠道可见性，权限锚点为应用而非团队；数据面无 ADMIN 跳过。
- `channel-failover`: Channel 级（L1）运行时失败转移回路——候选列表路由、`ChannelFailoverInvoker`、错误分流表、流式转移边界。
- `resilience-profile`: 应用级容灾画像——`ResilienceProfile` 实体与纯数据库管理、解析链（Application→Global）、容灾模式档位推导、会话亲和。
- `cluster-failover`: Cluster 故障域分组——显式 Cluster 实体、域级健康聚合、共因隔离与亲和路由。
- `resilience-console`: 容灾管理控制台——总览/模板/应用管理 + 一键应急操作。

### Modified Capabilities
- `intelligent-degradation`: `degrade(reason)` 由不分流改为按错误类型分流，定位由「全局主容灾」降级为「L2 应用可选兜底」；`enableL2ModelDegradation`/`degradationMaxDepth` 受画像门禁。
- `model-instance`: `InstanceSelector.select` 由返回单实例改为返回排序候选列表；`priority` 语义重定义为 cluster 内排序，cluster 间由 Cluster 优先级决定。
- `channel-health-tracking`: 熔断 key 统一为 `endpointId`，路由侧与调用侧共享熔断器；`ProviderHealthTracker` 职责收窄为供应商级粗粒度信号。
- `upstream-exception-classification`: 错误分类结果接入错误分流表，驱动 L1/L2/NONE 转移决策。

### Removed Capabilities
- `team-channel-management`: Team 体系移除，权限主体从团队改为应用，由 `application-access-control` 取代。需数据迁移。

## Impact

**领域层（supply 域 + iam 域）**：
- 新增实体 `Application`、`ApplicationChannel`、`ResilienceProfile`、`Cluster`。
- 移除实体 `Team`、`UserTeam`、`TeamChannel` 及相关 Gateway。
- `UserApiKey` 增加 `application_id`（权限锚点）；`Channel` 增加 `cluster_id`；`ModelInstance.priority` 语义变更。

**应用层（proxy 路由与调用）**：
- `RouterChain` 顺序与熔断 key 修正（P0）。
- `InstanceSelector`、`RoutingResolver` 改造为候选列表（P1）。
- `PermissionRouter` 重写：过滤依据从 `UserTeam → TeamChannel` 改为 `Application → ApplicationChannel`，移除 ADMIN 跳过分支（P-r）。
- 新增 `ChannelFailoverInvoker`，`DegradationInvoker` 退场或降级为内部组件。
- `RoutingRequest` 增加 `resilienceProfile` 字段贯穿 RouterChain 与 Invoker 链。
- 新增 `ResilienceResolver`（解析链 Application→Global）、`PinnedModelRouter`、`ClusterAffinityRouter`。

**适配层（adapter）**：
- `ApiKeyAuthInterceptor` 认证后经 `UserApiKey.application_id` → `Application` 解析权限边界与画像，塞入上下文。
- 新增 Application/画像/Cluster/容灾事件/应用访问控制的 Controller 与 DTO；移除 Team 相关 Controller。

**控制台（gateway-console）**：

前端依赖顺序完全跟随后端 P-r → P0 → P1 → P2，各阶段 API 落地后跟进，不单独排期。

| 后端阶段 | 前端工作 | 量级 |
|---------|---------|------|
| P-r | Teams 页改造为 Applications 页（建/编辑应用、绑 Key、渠道授权平移）；**移除成员管理**（`MemberManageModal` 删除）；**移除模型可见性**（`ModelVisibilityModal` 删除，D8）；路由 `teams`→`applications`、菜单项、权限常量新增 `APPLICATION_READ/WRITE`、清理 team.ts/useTeams.ts | 重 |
| P0 | 无前端（纯后端路由修正） | 无 |
| P1 | 无直接前端；P2 总览页消费其转移事件流 | 无 |
| P2 | 画像模板页（CRUD，专家字段折叠）；Applications 页加「容灾模式」档位选择 + 降级兜底开关；容灾总览页（故障域拓扑 + 实时转移事件流 + 耗尽告警）；Channels 页一键手动熔断/恢复/紧切域应急按钮 | 中 |

关键点：
- P-r 前端是「Teams 页功能平移到 Applications 页」而非从零建，渠道授权/模型可见性/Key 管理已有现成 Modal 可迁移，降低工作量。
- 成员管理不保留——去 Team 后无「成员」概念，应用仅由其归属的 Key 决定可用性。
- 权限常量新增 `APPLICATION_READ/WRITE`、`RESILIENCE_READ/WRITE`；现有无 `TEAM_*` 常量，主要清理 `teams` 路由与菜单项及 `services/api/team.ts`、`useTeams.ts`。

**配置与数据**：
- 新增 `applications`、`application_channels`、`application_models`、`resilience_profiles`、`clusters` 表。
- 移除 `teams`、`user_teams`、`team_channels` 表（迁移后）。
- `user_api_keys`、`channels` 增列；画像预设档位初始化数据（复用 `SampleDataLoader`/`BuiltinVendorLoader` 模式）。
- **数据迁移脚本**：现有 Team/TeamChannel → Application/ApplicationChannel，按应用归属重建授权。

**测试**：
- 路由器顺序、熔断 key 统一、L1 转移回路、错误分流、画像解析链、Cluster 健康聚合的单元与集成测试。
- 权限重构：ApplicationChannel 过滤、权限锚点切换、无 ADMIN 跳过、数据迁移正确性测试。
- 两对照场景（Claude Code 禁降级 / 客服全开）端到端验证；多 Key 共用 Application 画像继承与权限继承验证。

**执行顺序与依赖**：
- 权限重构 P-r 最优先（地基：Application 实体、访问控制、去 Team、数据迁移）。
- 容灾 P0/P1 紧随 P-r（PermissionRouter 已是终态，候选来源即 ApplicationChannel，无需二次重写）。
- 容灾 P2（画像+Cluster）最后在 Application 权限上落地（画像挂 Application、Cluster 按 Application 边界亲和）。
- 依赖链：P-r → P0 → P1 → P2。

**非目标（本 change 不做）**：
- Application 聚根本 change 仅启用权限+画像承载 + 预留配额/预算与用量看板字段（留空）。应用级配额/预算池的实际计费逻辑、应用级用量成本看板的实际呈现、应用级密钥轮换均**不实做**，留待 `quota`/`audit` 域填充预留字段。
- 不重写上游客户端与重试/熔断算法本身（复用现有 `ResilientUpstreamClient`/`CircuitBreaker`）。容灾只在调度层加转移决策，传输层韧性机制复用不改。
- 不做 Application 跨组织共享（移除 Team 后无「跨 Team」概念；若将来需应用共享，另立机制）。
