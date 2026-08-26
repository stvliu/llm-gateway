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
- P-r：Application 升级为权限+行为根实体，移除 Team，权限锚点从 userId 改为 applicationId；渠道/模型可见性归 Application（ApplicationChannel/ApplicationModel）。
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

### D4: Application 为权限+行为双根实体
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
