# Comet Design Handoff

- Change: resilience-architecture
- Phase: design
- Mode: compact
- Context hash: c7e386d13636555034f47a02e04c13ed158648a6bb8bb6f6a1d32533fed0defe

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/resilience-architecture/proposal.md

- Source: openspec/changes/resilience-architecture/proposal.md
- Lines: 1-124
- SHA256: e3fa6e238469d264878932aeb0d0371bc0665149072a48cda037f893550f52fe

[TRUNCATED]

```md
## Why

当前网关存在两类核心缺口：

**容灾缺口**：只有 Key 级转移（同渠道换 Key）和模型级降级（换模型）两层，**缺失 Channel 级运行时失败转移**——同模型多渠道间无法运行时按优先级自动切换，次优先级渠道永不参与转移；模型级降级对共因故障（认证/配额/账号限流/网络/宕机）无效却被当主容灾手段；熔断 key 路由侧（channelId）与调用侧（endpointId）不一致，健康过滤与实际熔断互不可见。

**权限模型缺口**：网关作为「应用聚合分发层」，真正的调用方是应用（Claude Code/OpenClaw/ERP），但现有权限模型以「人/团队」为授权对象（`User → Team → TeamChannel`），无法干净表达「不同应用访问不同渠道与模型」——不同应用要不同渠道只能靠为每个应用建 Team 迁就，污染组织模型。授权对象应从「人/团队」转为「应用」。

本 change 双线推进：将 Application 升级为权限+行为聚合根，移除 Team，使「应用」成为渠道/模型可见性与容灾画像的统一决策者；并建立四层容灾栈（Key→Channel→模型→兜底）补齐 Channel 级透明容灾。

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
- 新增 `Application` 聚合根实体 + `applications` 表，作为「权限 + 行为」双聚合根：承载 N 把 Key 的应用归属、渠道可见性、模型可见性、容灾画像。
- `UserApiKey` 增加 `application_id`，权限锚点从 `userId`（人）改为 `apiKeyId` → `applicationId`（应用）。
- 渠道可见性归 Application：新增 `ApplicationChannel`（应用能访问哪些渠道）。**模型可见性不独立配置**——由渠道上挂哪些 ModelInstance 隐式决定，废弃现有「团队模型可见性」机制。权限链重写为 `UserApiKey → Application → ApplicationChannel → Channel`。
- **移除 Team 体系**：删除 `Team`、`UserTeam`、`TeamChannel` 实体及相关 Gateway；`team-channel-management` capability 作废。现有 Team/TeamChannel 数据 1:1 平移到 Application/ApplicationChannel（授权不丢），归属不明 Key 归 `migration-default` 应用。
- `PermissionRouter` 重写：过滤依据从 `UserTeam → TeamChannel` 改为 `UserApiKey.application_id → ApplicationChannel`；**移除 ADMIN 跳过分支**（ADMIN 退回管理面特权，数据面无跳过）。
- Application 预留配额/预算、用量看板字段（留空，待 `quota`/`audit` 域填充）；密钥轮换不建。

### P2 应用级容灾画像 + Cluster 故障域分组（原 P2/P3 合并）—— 容灾线，等权限重构
- 新增 `ResilienceProfile` 领域实体 + `resilience_profiles` 表，承载四层栈开关、错误分流覆盖、模型锁定、会话亲和、成本/超时策略。**纯数据库方案**：画像全部落库 + CRUD API + 控制台管理，预设档位（default/strict/aggressive/batch）由初始化数据写入，满足「全实体可审计」铁律。
- `Application` 挂 `resilience_profile_id` 承载画像。解析链 `Application → Global`（Team 已移除，无中间层；Application 画像为主，全局 `default` 兜底）。
- `ResilienceProfile` 暴露给管理员两个面向字段：容灾模式档位（STANDARD/STRICT/AGGRESSIVE，BATCH 为 STANDARD 的 QUEUED 变体）+ 降级兜底开关，其余专家字段由档位自动推导。
- 会话亲和：场景1（agent 多轮）按请求头 `X-Session-Id` 亲和到首次命中渠道，标识缺失时不亲和（安全降级）。SessionAffinityStore 接口 + Redis(生产)/InMemory(开发) 双实现，TTL 30min，亲和优先非强制（熔断则转移并更新）。
- 修正 `DegradationService.degrade(reason)` 语义：按 reason 分流，定位为 L2 兜底（应用可选启用），非全局主路径。
- 新增显式 `Cluster` 领域实体 + `clusters` 表（code/name/provider_id/region/priority/health_status），`Channel.cluster_id` FK（直接建实体，不经软字段阶段）。Cluster 级健康聚合：域内所有渠道熔断 → Cluster DOWN，路由跳过整域；域内任一渠道 half-open 成功 → Cluster 解除 DOWN。Cluster 既是故障域（共因隔离）也是亲和域（就近低延迟 / 按供应商分域锁定模型）。

### 控制台管理范式
- **屏1 容灾总览**（Dashboard 增强，只读）：故障域拓扑 + 实时转移事件流 + 耗尽告警。
- **屏2 画像模板**：模板 CRUD，专家字段折叠在「高级」，管理员克隆微调而非从零填。
- **屏3 应用管理**：新增 Applications 管理页（建应用、绑 Key、配渠道/模型可见性、选容灾模式档位）；Channels 页加一键手动熔断/恢复/紧切域应急操作（复用熔断器，无新状态机）；移除原 Teams 管理页。

## Capabilities

### New Capabilities
- `application`: 应用聚合根——权限+行为双聚合，承载 Key 归属、渠道/模型可见性、容灾画像，预留配额/看板字段（本 change 启用权限+画像，配额/看板留空）。
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
```

Full source: openspec/changes/resilience-architecture/proposal.md

## openspec/changes/resilience-architecture/design.md

- Source: openspec/changes/resilience-architecture/design.md
- Lines: 1-80
- SHA256: 0e94e6cf3870b7c5bb9b02ef04afa6ccdb3484090aab7c4ec66892137cc588df

```md
## Context

网关作为「应用聚合分发层」，下游调用方是应用（Claude Code/OpenClaw/ERP）而非人。现有两类核心缺口：

**权限模型错位**：现有授权对象是「人/团队」（`User → Team → TeamChannel → Channel`），无法干净表达「不同应用访问不同渠道与模型」——不同应用要不同渠道只能靠为每个应用建 Team 迁就，污染组织模型。

**容灾缺口**：仅 Key 级转移（同渠道换 Key）和模型级降级（换模型）两层，缺 Channel 级运行时失败转移；模型级降级对共因故障（AUTH/QUOTA/账号限流/网络/宕机）无效却被当主容灾手段；熔断 key 路由侧（channelId）与调用侧（endpointId）不一致。

**现状关键事实**：
- `PermissionRouter` 权限链：`userId → UserTeam → teamId → TeamChannel → channelId`，ADMIN 跳过。
- `RouterChain` 顺序 `Permission(@100) → Priority(@200) → Health(@300) → LoadBalance(@9999)`，Priority 先于 Health 且 force，导致次优先级渠道永不被选。
- 熔断 key 不一致：`HealthRouter` 用 channelId，`KeyFailoverInvoker` 用 endpointId。
- 前端 Teams 页已承载渠道授权/模型可见性/Key 管理（`ChannelManageModal`/`ModelVisibilityModal`/`UserApiKeyManageModal`），P-r 为功能平移而非从零建。
- 前端权限常量无 `TEAM_*`，Team 在前端本就弱。

设计依据见 `doc/容灾方案设计.md` 与 `doc/容灾管理范式.md`。

## Goals / Non-Goals

**Goals:**
- P-r：Application 升级为权限+行为聚合根，移除 Team，权限锚点从 userId 改为 applicationId；渠道/模型可见性归 Application（ApplicationChannel/ApplicationModel）。
- P0：修正 RouterChain 顺序（Health 先于 Priority），统一熔断 key 为 endpointId。
- P1：ChannelFailoverInvoker + 候选列表路由 + 错误分流表，补齐 Channel 级 L1 透明容灾。
- P2：ResilienceProfile 纯数据库管理，挂 Application；容灾模式档位推导；会话亲和（X-Session-Id）。
- P3：显式 Cluster 实体 + 域级健康聚合 + 共因隔离/亲和路由。
- 前端：Teams 页改造为 Applications 页，三屏管理范式。

**Non-Goals:**
- Application 配额/预算池、用量成本看板、密钥轮换不实做（预留字段，留 quota/audit 域）。
- 不重写上游客户端与重试/熔断算法（复用 ResilientUpstreamClient/CircuitBreaker）。
- 不做 Application 跨组织共享（去 Team 后无「跨 Team」概念）。
- 不保留应用成员管理（应用只管 Key 归属，谁持 Key 谁能用）。

## Decisions

### D1: 权限锚点从「人/团队」转为「应用」
授权对象是应用而非人。权限链重写为 `UserApiKey → Application → ApplicationChannel/ApplicationModel → Channel/Model`。Team 体系（Team/UserTeam/TeamChannel）移除，数据迁移到 Application/ApplicationChannel。理由：网关下游是应用，「不同应用访问不同渠道」只有应用为权限主体才能干净表达；正确性优先于容灾，P-r 为地基最优先。

### D2: 执行顺序 P-r → P0 → P1 → P2（四段）
权限重构是地基，先建终态 PermissionRouter，容灾候选来源无需从 Team 切到 Application 造成回归；Team 数据迁移宜早不宜晚。P0/P1 紧随（共享终态权限），P2（画像+Cluster）最后在 Application 权限上落地。原 P2/P3 合并为一段（见 D10）。

### D3: 四层容灾栈 + 错误分流
L0 Key 级（同渠道换 Key，已有）→ L1 Channel 级（同模型换渠道，对用户透明，本 change 核心）→ L2 模型级（换能力近似模型，应用可选兜底）→ L3 抛错。错误分流表：INVALID_REQUEST 绝不转移，其余按 ProviderErrorType 映射 L1/L2/NONE。共因故障（AUTH/QUOTA/账号限流/网络/宕机）走 L1 换渠道而非 L2 换模型。

### D4: Application 为权限+行为双聚合根
Application 承载：Key 归属（N 把 Key 共用一个应用）、渠道可见性（ApplicationChannel）、容灾画像（resilience_profile_id）。**不独立配模型可见性**（见 D8），不保留成员管理。归属关系：Key → Application（无 Team 中间层）。

### D5: 画像纯数据库管理 + 解析链 Application → Global
ResilienceProfile 全部落库 + CRUD + 控制台，预设档位（default/strict/aggressive/batch）由初始化数据写入。解析链 Application → Global（Team 已移除，无中间层）。管理员面向两字段：容灾模式档位（STANDARD/STRICT/AGGRESSIVE，BATCH 为 STANDARD 的 QUEUED 变体）+ 降级兜底开关，专家字段由档位自动推导。

### D6: 前端 Teams → Applications 平移
Teams 页功能平移到 Applications 页（渠道授权/Key 管理已有现成 Modal）；移除成员管理；**移除模型可见性 Modal**（见 D8）；前端依赖顺序跟随后端各阶段 API。

### D7: 数据迁移 1:1 平移 + 兜底应用
1 Team → 1 默认 Application（code/name 继承），TeamChannel → ApplicationChannel 1:1 平移，授权不丢不失真。应用细分（ClaudeCode/IPD）留运维后续手动。归属不明 Key（多 Team 用户等）归 `migration-default` 应用挂 default 画像，运行期无 application_id 的 Key 软兜底+告警。迁移脚本可重跑、幂等、迁移前后授权集合比对校验。

### D8: 模型从属渠道，砍模型可见性
只配 ApplicationChannel，模型可见性由「渠道上挂哪些 ModelInstance」隐式决定，不建独立 ApplicationModel。废弃现有「团队模型可见性」机制（`listAllowedModels` + 前端 `ModelVisibilityModal`）。无法「授权渠道但限模型」——要某模型就授权挂该模型的渠道。过滤链单层 ApplicationChannel。

### D9: ADMIN 退管理面，数据面无跳过
ADMIN 保留为管理面特权（Sa-Token `@SaCheckRole("ADMIN")` 管应用/渠道/画像配置）；数据面 `PermissionRouter` 一律走 ApplicationChannel，无 ADMIN 跳过后门。管理员调试用专门「全渠道调试应用」。migration-default 渠道范围按原 Team 渠道集授权（非全局开放，不放大权限）。

### D10: 合并 P2/P3，直接建 Cluster 实体
P2 直接建 Cluster 实体（clusters 表 + Channel.cluster_id FK），不经历 cluster_code 软字段阶段。原 P2（画像）+ P3（Cluster）合并。理由：P2 转移逻辑已需 cluster 概念，软字段与实体访问方式不同，分期会改两遍转移逻辑；Cluster 实体本身轻。路线图五段变四段。

## Risks / Trade-offs

**R1: 数据迁移风险**（高）：现有 Team/TeamChannel → Application/ApplicationChannel 迁移，需保证授权不丢不失真。缓解：迁移脚本可重跑、迁移前后授权集合比对校验、保留回滚；migration-default 兜底不阻断业务。

**R2: 权限锚点切换兼容性**（高）：从 userId 改为 applicationId 涉及 PermissionRouter、RoutingRequest、拦截器等多处。缓解：P-r 完整覆盖所有触点，集成测试验证；无 Application 的 Key 经 migration-default 软兜底。

**R3: 单 change 体量大**（中）：权限重构 + 四层容灾 + 画像 + Cluster + 前端，范围大周期长。缓解：严格依赖顺序 P-r→P0→P1→P2 四段分阶段交付与验证；任务粒度细化。

**R4: RouterChain 顺序变更 BREAKING**（中）：次优先级渠道行为改变。缓解：明确为预期修正，测试覆盖转移场景。

**R5: 移除 Team 的前端/后端清理面**（中）：Team 相关 Gateway/Controller/路由/菜单/服务文件多。缓解：P-r 任务清单逐项清理，grep 确认无残留引用。

**R6: 砍模型可见性的能力回退**（中）：废弃现有「授权渠道但限模型」能力。缓解：语义收敛到供给侧（渠道配置 ModelInstance），若确需应用级模型隔离则建不同渠道挂不同模型；记录为已知取舍。

**R7: ADMIN 数据面特权移除的运维适应**（低）：管理员不能再用 ADMIN Key 调任意渠道。缓解：建全渠道调试应用替代，可审计可回收。
```

## openspec/changes/resilience-architecture/tasks.md

- Source: openspec/changes/resilience-architecture/tasks.md
- Lines: 1-53
- SHA256: 024d23cc78cbd01bc9d4b000f1633e6a13333872e1c961bbfab6b907f3ae76d6

```md
# Tasks

> 依赖顺序：P-r → P0 → P1 → P2（四段，原 P2/P3 已合并）。前端任务跟随后端各阶段 API 落地。
> design 阶段深化结论：砍 ApplicationModel（D8）、ADMIN 退管理面（D9）、合并 P2/P3（D10）。

## 1. P-r 权限重构（最优先，地基）

- [ ] 1.1 新增 `Application` 聚合根实体 + `applications` 表（code/name/description/state + 审计字段 + 预留配额/看板字段留空）
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
```

