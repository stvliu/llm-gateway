# 韧性架构与权限重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

---
change: resilience-architecture
design-doc: docs/superpowers/specs/2026-06-19-resilience-architecture-design.md
base-ref: f6093b75eaaf961e0f3902bd41ebbdc7037e539a
---

**Goal:** 将 Application 升级为权限+行为聚合根、移除 Team 体系，并补齐 Channel 级（L1）透明容灾与应用级容灾画像/Cluster 故障域分组。

**Architecture:** 分四段顺序交付 P-r（权限重构地基）→ P0（路由与熔断错配修正）→ P1（L1 Channel 级转移回路）→ P2（画像+Cluster）。权限锚点从 userId 改为 applicationId，权限链重写为 `UserApiKey → Application → ApplicationChannel → Channel`；容灾栈四层（L0 Key→L1 Channel→L2 模型→L3 抛错），由 `ChannelFailoverInvoker` 在候选列表内逐个试，错误分流表决定 L1/L2/NONE。

**Tech Stack:** Java 21 + Spring Boot 3.5.x, Spring MVC, JPA/Hibernate, Flyway 迁移, H2(开发)/PostgreSQL(生产), Redis, JUnit 5 + Mockito, Sa-Token。

## Global Constraints

- COLA Light 5.0 单模块分层：domain 只依赖 Gateway 接口，Gateway 实现在 infrastructure；禁止跨层/反向依赖。
- 全实体可审计：每张业务表含 `created_by/created_at/updated_by/updated_at`。
- 表名 snake_case 复数；主键 `id BIGINT AUTO_INCREMENT`；外键 `*_id BIGINT`。
- public 方法必须中文 Javadoc；类/复杂逻辑中文注释；业务逻辑块中文解释。
- 双 API 兼容不可破坏：`/v1/chat/completions` 与 `/v1/messages` 行为保持。
- 不重写上游客户端与重试/熔断算法本身（复用 `ResilientUpstreamClient`/`CircuitBreaker`）。
- 所有可变参数走 `@ConfigurationProperties`，禁止魔法数字。
- 构建命令：`./mvnw -pl gateway-boot -am test`（测试）；`./mvnw -pl gateway-boot -am clean install -DskipTests`（仅构建）。
- Flyway 版本号续接现有最大 V50，本计划从 V51 起（Task 1.1 已用 V51）。后续：1.2 追加表入 V51 或新建、1.6 迁移 V52、1.7 删表 V53、4.1 画像 V54、4.2 Cluster V55、4.4 seed V56。实现时以仓库实际最大版本号 +1 为准。
- **实体风格铁律**：所有领域实体继承 `common/entity/BaseEntity`（已封装 id + createdBy/createdAt/updatedBy/updatedAt 审计字段），使用 `@Data + @EqualsAndHashCode(callSuper=true) + @DomainEntity`，参照 `domain/supply/entity/Channel.java`。实体类不得自包含重复声明审计字段。brief 中若列出审计字段仅作字段语义说明，实现时审计字段来自 BaseEntity 继承，业务字段单独声明。
- 涉及业务逻辑的任务遵循 TDD：先写失败测试，再实现。
- D8 决策：**不建 `ApplicationModel`/`application_models` 表**。模型可见性由渠道上挂哪些 ModelInstance 隐式决定。proposal 第105行的 `application_models` 项已被 design D8 覆盖作废，本计划不实现。

## File Structure

### 领域层新增/修改

- **Create** `domain/application/entity/Application.java` — 应用聚合根实体（code/name/description/state + 审计字段 + 预留 quota/profile 字段）
- **Create** `domain/application/entity/ApplicationChannel.java` — 应用-渠道授权关联实体
- **Create** `domain/application/gateway/ApplicationGateway.java` — 应用查询/持久化 Gateway 接口
- **Create** `domain/application/gateway/ApplicationChannelGateway.java` — 应用-渠道授权 Gateway 接口
- **Create** `domain/resilience/entity/ResilienceProfile.java` — 容灾画像实体
- **Create** `domain/resilience/entity/Cluster.java` — 故障域实体
- **Create** `domain/resilience/gateway/ResilienceProfileGateway.java` — 画像 Gateway 接口
- **Create** `domain/resilience/gateway/ClusterGateway.java` — Cluster Gateway 接口
- **Create** `domain/resilience/gateway/SessionAffinityStore.java` — 会话亲和存储接口
- **Modify** `domain/iam/entity/UserApiKey.java` — 增加 `applicationId` 字段
- **Modify** `domain/supply/entity/Channel.java` — 增加 `clusterId` FK
- **Delete** `domain/team/entity/Team.java`、`TeamChannel.java`、`UserTeam.java` 及 `domain/team/gateway/*` 全部
- **Delete** `domain/team/` 包整体（移除团队模型可见性 `listAllowedModels` 等）

### 应用层新增/修改

- **Create** `application/proxy/invoker/ChannelFailoverInvoker.java` — L1 Channel 级转移回路
- **Create** `application/proxy/failover/ErrorClassifier.java` — 错误分流表（ProviderErrorType → L1/L2/NONE）
- **Create** `application/resilience/ResilienceResolver.java` — 画像解析链 Application→Global
- **Create** `application/resilience/ResilienceProfileApplier.java` — 容灾模式档位 → Profile 字段推导
- **Create** `application/proxy/routing/PinnedModelRouter.java` — 锁定模型路由器
- **Create** `application/proxy/routing/ClusterAffinityRouter.java` — 就近/按域亲和路由
- **Create** `application/proxy/routing/ClusterHealthAggregator.java` — 域级健康聚合
- **Modify** `application/proxy/routing/RouterChain.java` — 顺序语义不变（靠 @Order）
- **Modify** `application/proxy/routing/PermissionRouter.java` — 改 ApplicationChannel 过滤，移除 ADMIN 跳过
- **Modify** `application/proxy/routing/HealthRouter.java` — 熔断 key 改 endpointId
- **Modify** `application/proxy/routing/PriorityRouter.java` — @Order 调整为 Health 之后
- **Modify** `application/proxy/routing/InstanceSelector.java` — `select` 返回候选列表
- **Modify** `application/proxy/routing/RoutingRequest.java` — 增加 applicationId/resilienceProfile
- **Modify** `application/proxy/RoutingResolver.java` — 适配候选列表
- **Modify** `application/proxy/ChatDispatchServiceImpl.java` — 接入 ChannelFailoverInvoker
- **Modify** `application/degradation/DegradationService(Impl).java` — degrade(reason) 按 reason 分流，L2 受画像门禁
- **Modify** `application/proxy/invoker/DegradationInvoker.java` — 退场或降级为内部组件

### 基础设施层新增/修改

- **Create** `infrastructure/application/gateway/ApplicationGatewayImpl.java`
- **Create** `infrastructure/application/gateway/ApplicationChannelGatewayImpl.java`
- **Create** `infrastructure/resilience/gateway/ResilienceProfileGatewayImpl.java`
- **Create** `infrastructure/resilience/gateway/ClusterGatewayImpl.java`
- **Create** `infrastructure/resilience/affinity/RedisSessionAffinityStore.java`
- **Create** `infrastructure/resilience/affinity/InMemorySessionAffinityStore.java`
- **Create** `infrastructure/application/persistence/ApplicationPO.java`、`ApplicationChannelPO.java`（若项目用 PO 模式）
- **Create** JPA Repository：`ApplicationRepository`、`ApplicationChannelRepository`、`ResilienceProfileRepository`、`ClusterRepository`
- **Create** `db/migration/V37__add_application_tables.sql` 等 Flyway 脚本

### 适配层新增/修改

- **Create** Application/ResilienceProfile/Cluster/ApplicationAccessControl Controller + DTO
- **Delete** Team 相关 Controller
- **Modify** `adapter/interceptor/ApiKeyAuthInterceptor.java` — 认证后解析 applicationId 塞入上下文

### 前端（gateway-console，跟随后端阶段）

- **Modify** Teams 页 → Applications 页；移除成员管理/模型可见性 Modal；路由 `teams`→`applications`
- **Create** 画像模板页 + 容灾总览页 + Channels 应急操作

---

## 里程碑与执行约定

- **四个里程碑**：P-r（任务 1.1–1.9）→ P0（2.1–2.4）→ P1（3.1–3.7）→ P2（4.1–4.11）→ 收尾（5.1–5.3）。
- 每段结束运行 `./mvnw -pl gateway-boot -am test` 确认全绿后再进下一段。
- 每个任务结束 commit 一次，message 体现设计意图（中文，符合 Conventional Commits）。
- TDD 任务：先写失败测试 → 跑红 → 实现 → 跑绿 → commit。非 TDD 任务（建实体/迁移脚本）直接实现 + 验证。

---

## 里程碑 P-r：权限重构（地基）

### Task 1.1: Application 聚合根实体 + applications 表

**Files:**
- Create: `gateway-boot/src/main/java/com/codingas/gateway/domain/application/entity/Application.java`
- Create: `gateway-boot/src/main/resources/db/migration/V37__add_application_tables.sql`（仅 applications 表部分）
- Test: `gateway-boot/src/test/java/com/codingas/gateway/domain/application/entity/ApplicationTest.java`

**Interfaces:**
- Produces: `Application` 实体，字段 `Long id; String code; String name; String description; ApplicationState state; Long resilienceProfileId; Long quotaBudgetId; Long dashboardId; Long createdBy; Instant createdAt; Long updatedBy; Instant updatedAt;`，构造器 + Getter/Setter（领域模型纯洁，无业务逻辑）。

- [x] **Step 1: 写实体与枚举**

创建 `domain/application/entity/ApplicationState.java` 枚举：`ACTIVE, INACTIVE`，带 `isRoutable()` 返回 `this == ACTIVE`。

创建 `Application.java`：Lombok `@Getter @Setter`，无参构造器，全参构造器（不含 id/审计字段）。中文 Javadoc 说明「应用聚合根：权限+行为双聚合，承载 Key 归属、渠道可见性、容灾画像，预留配额/看板字段」。

- [x] **Step 2: 写 Flyway 迁移**

`V37__add_application_tables.sql`：

```sql
CREATE TABLE applications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    state VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    resilience_profile_id BIGINT,
    quota_budget_id BIGINT,
    dashboard_id BIGINT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_applications_code ON applications(code);
```

- [x] **Step 3: 写测试**

`ApplicationTest.java`：验证 `ApplicationState.ACTIVE.isRoutable()` 为 true，`INACTIVE` 为 false；验证实体字段可读写。

- [x] **Step 4: 跑测试确认通过**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ApplicationTest`
Expected: PASS

- [x] **Step 5: Commit**

```bash
git add gateway-boot/src/main/java/com/codingas/gateway/domain/application/ \
        gateway-boot/src/main/resources/db/migration/V37__add_application_tables.sql \
        gateway-boot/src/test/java/com/codingas/gateway/domain/application/
git commit -m "feat(application): 新增 Application 聚合根实体与 applications 表"
```

### Task 1.2: ApplicationChannel 实体 + 表

**Files:**
- Create: `domain/application/entity/ApplicationChannel.java`
- Modify: `db/migration/V37__add_application_tables.sql`（追加 application_channels 表）
- Test: `domain/application/entity/ApplicationChannelTest.java`

**Interfaces:**
- Produces: `ApplicationChannel` 字段 `Long id; Long applicationId; Long channelId; Instant createdAt; Long createdBy;`。

- [x] **Step 1: 写实体**

`ApplicationChannel.java`：`@Getter @Setter`，中文 Javadoc「应用-渠道授权关联：决定应用可见的渠道集合」。

- [x] **Step 2: 追加迁移**

V37 末尾追加：

```sql
CREATE TABLE application_channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_channel (application_id, channel_id)
);
CREATE INDEX idx_app_channels_app ON application_channels(application_id);
```

- [x] **Step 3: 写测试并跑绿**

`ApplicationChannelTest.java` 验证字段读写。Run: `./mvnw -pl gateway-boot -am test -Dtest=ApplicationChannelTest` → PASS

- [x] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(application): 新增 ApplicationChannel 实体与 application_channels 表"
```

### Task 1.3: UserApiKey 增 application_id + Application/ApplicationChannel Gateway

**Files:**
- Modify: `domain/iam/entity/UserApiKey.java`
- Create: `domain/application/gateway/ApplicationGateway.java`
- Create: `domain/application/gateway/ApplicationChannelGateway.java`
- Create: `infrastructure/application/gateway/ApplicationGatewayImpl.java`
- Create: `infrastructure/application/gateway/ApplicationChannelGatewayImpl.java`
- Create: JPA Repository `ApplicationRepository.java`、`ApplicationChannelRepository.java`
- Create: PO（若需）`infrastructure/application/persistence/ApplicationPO.java`、`ApplicationChannelPO.java`
- Modify: `db/migration/V37__add_application_tables.sql`（追加 user_api_keys 增列）
- Test: `ApplicationGatewayImplTest.java`、`ApplicationChannelGatewayImplTest.java`

**Interfaces:**
- Produces:
  - `ApplicationGateway`: `Application findById(Long id); Application findByCode(String code); List<Application> findAll(); Application save(Application app);`
  - `ApplicationChannelGateway`: `Set<Long> findChannelIdsByApplicationId(Long appId); List<ApplicationChannel> findByApplicationId(Long appId); void saveAll(List<ApplicationChannel> rels); boolean existsByApplicationIdAndChannelId(Long appId, Long chId);`

**Consumes:** 现有 `ChannelGateway` 模式作为参照（见 `domain/supply/gateway/ChannelGateway.java` 及其 Impl）。

- [ ] **Step 1: UserApiKey 增字段**

在 `UserApiKey.java` 增加 `private Long applicationId;` + Getter/Setter，更新 Javadoc 说明权限锚点改为应用。迁移追加：

```sql
ALTER TABLE user_api_keys ADD COLUMN application_id BIGINT;
CREATE INDEX idx_user_api_keys_app ON user_api_keys(application_id);
```

- [ ] **Step 2: 写 Gateway 接口**

按上面 Produces 签名写两个接口，中文 Javadoc。

- [ ] **Step 3: 写 JPA Repository + PO + GatewayImpl**

参照现有 `infrastructure/supply/gateway/ChannelGatewayImpl.java` 模式（先 Read 该文件确认 PO/Repository 用法）。`ApplicationGatewayImpl` 实现接口，PO↔Entity 转换。

- [ ] **Step 4: 写测试并跑绿**

测试 GatewayImpl 的 `findById/findByCode/findChannelIdsByApplicationId`（用 H2 内存库或 Mockito Repository）。Run: `./mvnw -pl gateway-boot -am test -Dtest=ApplicationGatewayImplTest,ApplicationChannelGatewayImplTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(application): UserApiKey 挂 application_id 并实现 Application/ApplicationChannel Gateway"
```

### Task 1.4: 重写 PermissionRouter（ApplicationChannel 过滤，移除 ADMIN 跳过）

**Files:**
- Modify: `application/proxy/routing/PermissionRouter.java`
- Modify: `application/proxy/routing/RoutingRequest.java`（增 applicationId）
- Test: `application/proxy/routing/PermissionRouterTest.java`（重写）

**Interfaces:**
- Consumes: `ApplicationChannelGateway.findChannelIdsByApplicationId(Long)`、`RoutingRequest.getApplicationId()`
- Produces: `PermissionRouter.filter` 不再有 ADMIN 分支；`RoutingRequest` 增加 `applicationId` 字段与构造器。

- [ ] **Step 1: RoutingRequest 增 applicationId**

在 `RoutingRequest.java` 增加 `private final Long applicationId;`，新增构造器 `RoutingRequest(Long modelId, Long applicationId, Long userId, String role, RoutingStrategy strategy)`，保留旧构造器（设 applicationId=null 并标 `@Deprecated`）。加 Getter。

- [ ] **Step 2: 写失败测试**

`PermissionRouterTest.java`：
- `normalApplication_filtersByApplicationChannel`：给定 applicationId=1，ApplicationChannel 返回 {ch1,ch2}，候选含 ch1/ch3，断言只剩 ch1。
- `noApplication_returnsEmpty`：applicationId=null，断言返回空。
- `admin_doesNotSkip`：role=ADMIN 且 applicationId=1，断言仍按 ApplicationChannel 过滤（无跳过）。
- `inactiveChannel_filtered`：channel state 非 routable 被过滤。

- [ ] **Step 3: 跑红**

Run: `./mvnw -pl gateway-boot -am test -Dtest=PermissionRouterTest` → FAIL（仍走旧 UserTeam 逻辑）

- [ ] **Step 4: 重写 PermissionRouter**

注入 `ApplicationChannelGateway` 替换 `UserTeamGateway/TeamChannelGateway`。`getPermittedChannelIds` 改为：若 applicationId 为 null 返回空；否则 `applicationChannelGateway.findChannelIdsByApplicationId(applicationId)`。删除 ADMIN 分支。保留活跃 Channel 过滤。@Order(100) 不变。

- [ ] **Step 5: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=PermissionRouterTest` → PASS

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(routing): PermissionRouter 改 ApplicationChannel 过滤并移除 ADMIN 跳过"
```

### Task 1.5: 拦截器权限锚点 userId→applicationId

**Files:**
- Modify: `adapter/interceptor/ApiKeyAuthInterceptor.java`
- Modify: `domain/iam/service/AuthenticationDomainService.java` 及 `Identity` valueobject（加 applicationId）
- Modify: `application/proxy/RoutingResolver.java`、`InstanceSelector.select` 调用点（传 applicationId）
- Modify: `application/proxy/ChatDispatchServiceImpl.java`（构造 RoutingRequest 用 applicationId）
- Test: `ApiKeyAuthInterceptorTest.java`（如有）、`ChatDispatchServiceTest.java` 适配

**Interfaces:**
- Consumes: `UserApiKey.applicationId`（Task 1.3）
- Produces: `Identity` 携带 `applicationId`；`RoutingRequest` 构造用 applicationId。

- [ ] **Step 1: Identity 增 applicationId**

Read `domain/iam/valueobject/Identity.java`，增加 `applicationId` 字段与构造器参数。`AuthenticationDomainService.authenticateUser` 返回的 Identity 填充 `userApiKey.getApplicationId()`。

- [ ] **Step 2: 适配 RoutingResolver/InstanceSelector/ChatDispatchService**

将所有 `new RoutingRequest(modelId, userId, role, strategy)` 改为 `new RoutingRequest(modelId, identity.getApplicationId(), userId, role, strategy)`。`InstanceSelector.select` 签名增 `applicationId` 参数（或从 RoutingRequest 取）。先 grep 所有 `new RoutingRequest(` 调用点确认覆盖。

- [ ] **Step 3: 写/改测试并跑绿**

适配 `ChatDispatchServiceTest` 等。Run: `./mvnw -pl gateway-boot -am test -Dtest=ChatDispatchServiceTest,ApiKeyAuthInterceptorTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(auth): 权限锚点从 userId 切换为 applicationId 贯穿拦截器与调度"
```

### Task 1.6: 数据迁移脚本（Team→Application 1:1 平移 + migration-default 兜底）

**Files:**
- Create: `db/migration/V38__migrate_team_to_application.sql`
- Create: `application/init/ApplicationMigrationLoader.java`（或纯 SQL 迁移，按现有迁移脚本风格）
- Test: `ApplicationMigrationTest.java`（H2 跑迁移，比对授权集合）

**Interfaces:**
- Consumes: 现有 `teams`/`user_teams`/`team_channels` 表数据
- Produces: `applications`/`application_channels` 数据 + `user_api_keys.application_id` 回填 + `migration-default` 兜底应用。

**迁移语义（D7/D9）**：
1. 1 Team → 1 默认 Application（code/name 继承 Team，code 加前缀避免冲突）。
2. TeamChannel → ApplicationChannel 1:1 平移。
3. UserApiKey.application_id 回填为其用户所属 Team 对应的 Application。
4. 归属不明 Key（多 Team 用户或无 Team）归 `migration-default` 应用，按原 Team 渠道集授权（取并集，非全局）。
5. 可重跑幂等：用 `INSERT ... SELECT ... WHERE NOT EXISTS`。

- [ ] **Step 1: 写迁移 SQL**

`V38__migrate_team_to_application.sql`：

```sql
-- 1. 兜底应用
INSERT INTO applications (code, name, description, state, created_at, updated_at)
SELECT 'migration-default', '迁移兜底应用', '归属不明 Key 兜底', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM applications WHERE code='migration-default');

-- 2. Team → Application（code 加 team- 前缀）
INSERT INTO applications (code, name, description, state, created_at, updated_at)
SELECT CONCAT('team-', t.code), t.name, t.description, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM teams t
WHERE NOT EXISTS (SELECT 1 FROM applications a WHERE a.code = CONCAT('team-', t.code));

-- 3. TeamChannel → ApplicationChannel
INSERT INTO application_channels (application_id, channel_id, created_at, updated_at)
SELECT a.id, tc.channel_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM team_channels tc
JOIN teams t ON tc.team_id = t.id
JOIN applications a ON a.code = CONCAT('team-', t.code)
WHERE NOT EXISTS (
  SELECT 1 FROM application_channels ac
  WHERE ac.application_id = a.id AND ac.channel_id = tc.channel_id
);

-- 4. 归属不明 Key 的渠道并集 → migration-default
INSERT INTO application_channels (application_id, channel_id, created_at, updated_at)
SELECT a.id, tc.channel_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_api_keys k
JOIN user_teams ut ON k.user_id = ut.user_id
JOIN team_channels tc ON tc.team_id = ut.team_id
JOIN applications a ON a.code = 'migration-default'
WHERE k.application_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM application_channels ac
    WHERE ac.application_id = a.id AND ac.channel_id = tc.channel_id
  );

-- 5. UserApiKey.application_id 回填
UPDATE user_api_keys k
JOIN user_teams ut ON k.user_id = ut.user_id
JOIN teams t ON ut.team_id = t.id
JOIN applications a ON a.code = CONCAT('team-', t.code)
SET k.application_id = a.id
WHERE k.application_id IS NULL;

-- 6. 仍无 application_id 的 Key 归 migration-default
UPDATE user_api_keys k
JOIN applications a ON a.code = 'migration-default'
SET k.application_id = a.id
WHERE k.application_id IS NULL;
```

> 注：实际字段名以 teams 表为准，实现前先 Read 一个 V*__*.sql 确认 teams/user_teams 字段。若 teams 无 code 列则用 id 生成 code。

- [ ] **Step 2: 写测试**

`ApplicationMigrationTest.java`：用 H2 建初始 teams/user_teams/team_channels/user_api_keys 数据，跑 V37+V38，断言：
- 每个 Team 对应一个 Application，TeamChannel 平移到 ApplicationChannel，授权集合相等。
- 多 Team 用户的 Key 归 migration-default，其渠道集为原 Team 渠道并集。
- 重跑 V38 不产生重复（幂等）。

- [ ] **Step 3: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ApplicationMigrationTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(migration): Team→Application 1:1 平移迁移脚本与 migration-default 兜底"
```

### Task 1.7: 移除 Team 体系实体与 Gateway

**Files:**
- Delete: `domain/team/` 整个包（Team/UserTeam/TeamChannel 实体 + Gateway 接口）
- Delete: `infrastructure/team/` 对应实现
- Delete: Team 相关 Controller（`adapter/api/TeamController.java` 等）
- Modify: 所有引用 Team/UserTeam/TeamChannel 的类（grep 确认）
- Modify: 移除 `listAllowedModels`（团队模型可见性，D8）
- Create: `db/migration/V39__drop_team_tables.sql`
- Test: 全量编译通过

- [ ] **Step 1: grep 所有 Team 引用**

Run: `grep -rn "domain.team\|UserTeamGateway\|TeamChannelGateway\|TeamGateway\|listAllowedModels" gateway-boot/src/main` 列出所有引用点。

- [ ] **Step 2: 删除 Team 包与实现**

删除 `domain/team/`、`infrastructure/team/`、Team Controller。逐个修复 grep 出的引用点（改为 Application 等价物或移除）。

- [ ] **Step 3: 移除模型可见性机制**

删除 `listAllowedModels` 方法及调用点（前端 ModelVisibilityModal 在 Task 1.9 删）。

- [ ] **Step 4: 删表迁移**

`V39__drop_team_tables.sql`：

```sql
DROP TABLE IF EXISTS team_channels;
DROP TABLE IF EXISTS user_teams;
DROP TABLE IF EXISTS teams;
```

- [ ] **Step 5: 全量编译+测试**

Run: `./mvnw -pl gateway-boot -am test` → 全绿（修复残余引用）

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "refactor(team): 移除 Team 体系实体/Gateway/Controller 并废弃团队模型可见性"
```

### Task 1.8: P-r 单元与集成测试

**Files:**
- Test: `integration/PermissionRefactorIntegrationTest.java`（端到端权限锚点切换）
- 补充 `PermissionRouterTest` 无 ADMIN 跳过、迁移正确性已在 1.6 覆盖。

- [ ] **Step 1: 写集成测试**

`PermissionRefactorIntegrationTest.java`：
- 应用 A 授权 ch1，应用 B 授权 ch2；用应用 A 的 Key 请求模型 M（ch1+ch2 都挂 M），断言只能路由到 ch1。
- 无 application_id 的 Key（模拟遗漏）走 migration-default 软兜底 + 告警日志。
- ADMIN Key 数据面不跳过，仍按 ApplicationChannel 过滤。

- [ ] **Step 2: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=PermissionRefactorIntegrationTest` → PASS

- [ ] **Step 3: P-r 全段回归**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "test(permission): P-r 权限重构集成测试与端到端验证"
```

### Task 1.9: 前端 Teams→Applications 页改造

**Files:**
- Modify/Move: `gateway-console/src/pages/teams/*` → `pages/applications/*`
- Delete: `MemberManageModal`、`ModelVisibilityModal`
- Modify: 路由 `teams`→`applications`、菜单项、权限常量增 `APPLICATION_READ/WRITE`、清理 `team.ts`/`useTeams.ts`
- 依赖：后端 Application Controller（P-r 内提供，若 Controller 未单独建则在 Task 1.7 附近补建 ApplicationController）

> 前端任务较大，可作为 P-r 末尾的独立子段，依赖后端 Application CRUD API。若后端 Controller 尚未建，先补建 `adapter/api/ApplicationController.java` + DTO（建/编辑/列表/绑 Key/渠道授权）。

- [ ] **Step 1: 补建后端 ApplicationController + DTO**（若未建）

参照现有 Controller 模式（Read 一个如 `ChannelController.java`），实现 Application CRUD + 渠道授权绑定 API。

- [ ] **Step 2: 前端页面平移**

`teams` 目录重命名为 `applications`，复用现有 `ChannelManageModal`/`UserApiKeyManageModal`，删除 `MemberManageModal`/`ModelVisibilityModal`。

- [ ] **Step 3: 路由/菜单/权限常量**

更新 `services/api/team.ts`→`application.ts`、`useTeams.ts`→`useApplications.ts`；权限常量增 `APPLICATION_READ/WRITE`。

- [ ] **Step 4: 前端构建验证**

Run: `cd gateway-console && npm run build`（或项目实际命令）→ 通过

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(console): Teams 页改造为 Applications 页并移除成员管理/模型可见性"
```

---

## 里程碑 P0：路由与熔断错配修正

### Task 2.1: 修正 RouterChain 顺序 Permission→Health→Priority→(Pinned/Cluster)→LoadBalance

**Files:**
- Modify: `application/proxy/routing/HealthRouter.java`（@Order 200）
- Modify: `application/proxy/routing/PriorityRouter.java`（@Order 300）
- Test: `RouterChainTest.java`（顺序断言）

**Interfaces:** 仅 @Order 调整，无新接口。

**语义**：当前 Permission(100)→Priority(200)→Health(300)→LoadBalance(9999)。改为 Permission(100)→Health(200)→Priority(300)→LoadBalance(9999)。Pinned/Cluster Router 在 P2 加（@Order 250/350 区间）。

- [ ] **Step 1: 写失败测试**

`RouterChainTest.java` 增 `routers_executedInOrder`：用 mock Router 带 @Order，断言执行顺序为 Permission→Health→Priority→LoadBalance。验证「次优先级健康渠道被选」场景：两渠道 ch1(priority=1,熔断)、ch2(priority=2,健康)，Health 先过滤掉 ch1，Priority 在剩余 ch2 上选，断言选 ch2。

- [ ] **Step 2: 跑红**

Run: `./mvnw -pl gateway-boot -am test -Dtest=RouterChainTest` → FAIL

- [ ] **Step 3: 调整 @Order**

`HealthRouter` @Order(200)，`PriorityRouter` @Order(300)。

- [ ] **Step 4: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=RouterChainTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "fix(routing): RouterChain 顺序改为 Permission→Health→Priority 使次优先级健康渠道可被选"
```

### Task 2.2: 统一熔断 key 为 endpointId

**Files:**
- Modify: `application/proxy/routing/HealthRouter.java`
- Modify（确认共享熔断器）: `infrastructure/resilience/ChannelEndpointCircuitBreakerManager.java`
- Test: `HealthRouterTest.java`、`KeyFailoverInvoker` 与 HealthRouter 共享熔断验证

**Interfaces:**
- Consumes: `ModelInstance.getChannelEndpointId()`（确认该字段存在，见 `domain/supply/entity/ModelInstance.java` 与 `RoutingContext.channelEndpointId()`）
- Produces: `HealthRouter.filter` 用 `circuitBreakerManager.isAvailable(mi.getChannelEndpointId())`。

> 现状 `HealthRouter` 用 `mi.getChannelId()`，`KeyFailoverInvoker` 用 `ctx.channelEndpointId()`。需确认 ModelInstance 是否有 endpointId 字段；若无，用 `mi.getChannelId()` 派生或确认 ModelInstance 与 Endpoint 关联。实现前 Read `ModelInstance.java`。

- [ ] **Step 1: 确认 endpointId 来源**

Run: codegraph explore `ModelInstance channelEndpointId RoutingContext channelEndpointId` 确认字段。

- [ ] **Step 2: 写失败测试**

`HealthRouterTest.java`：同一 channel 的两个 endpoint，ep1 熔断 ep2 健康，断言 HealthRouter 过滤掉 ep1 实例保留 ep2。验证 HealthRouter 与 KeyFailoverInvoker 用同一 `circuitBreakerManager` bean（共享熔断器实例）。

- [ ] **Step 3: 跑红** → FAIL

- [ ] **Step 4: 改 HealthRouter**

`filter` 改为按 endpointId 查熔断。若 ModelInstance 无 endpointId，需先在实体加字段并迁移（子步骤）。

- [ ] **Step 5: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=HealthRouterTest,KeyFailoverInvokerTest` → PASS

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "fix(resilience): 熔断 key 统一为 endpointId，HealthRouter 与 KeyFailoverInvoker 共享熔断器"
```

### Task 2.3: ProviderHealthTracker 职责收窄为供应商级粗粒度信号

**Files:**
- Modify: `infrastructure/actuator/ProviderHealthTracker.java`
- Modify: 移除其在 L1 路由决策中的调用（grep 调用点）
- Test: `ProviderHealthTrackerTest.java`（如有）

**语义（D3）**：`ProviderHealthTracker` 不再驱动 L1 路由（L1 由 endpoint 级熔断驱动）。仅保留供应商级 DOWN 信号，供 L2 备选模型可用性判断。grep 其调用点，移除路由侧使用。

- [ ] **Step 1: grep 调用点**

Run: `grep -rn "ProviderHealthTracker\|providerHealthTracker" gateway-boot/src/main`

- [ ] **Step 2: 移除路由侧调用，更新 Javadoc**

Javadoc 改为「供应商级粗粒度健康信号，仅供 L2 备选模型可用性判断，不驱动 L1 路由」。

- [ ] **Step 3: 测试 + 跑绿**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "refactor(resilience): ProviderHealthTracker 收窄为供应商级粗粒度信号，不再驱动 L1 路由"
```

### Task 2.4: P0 单元测试

**Files:**
- 补充 `RouterChainTest`（顺序）、`HealthRouterTest`（熔断 key 一致）、次优先级渠道被选场景。

- [ ] **Step 1: 补全 P0 测试**

确保 2.1/2.2 测试覆盖：路由顺序、次优先级渠道被选、熔断 key 一致。

- [ ] **Step 2: P0 全段回归**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test(resilience): P0 路由顺序与熔断 key 统一单元测试"
```

---

## 里程碑 P1：L1 Channel 级转移回路（核心）

### Task 3.1: RouterChain 联合产出候选列表

**Files:**
- Modify: `application/proxy/routing/RouterChain.java`（filter 仍返回列表，语义不变）
- Modify: `application/proxy/routing/LoadBalanceRouter.java`（退场为可选，或保留作 fallback）
- Modify: `application/proxy/routing/InstanceSelector.java`（select 返回 List）
- Test: `InstanceSelectorTest.java`（重写）

**Interfaces:**
- Produces: `InstanceSelector.select(...)` 返回 `List<ModelInstance>`（按 cluster/priority 排序的候选列表），而非单个。
- Consumes: `RouterChain.filter` 返回排序列表（LoadBalanceRouter 不再强制收敛到单实例）。

> D5/深化点5：RouterChain(Permission+Health+Priority) 产出候选列表；LoadBalanceRouter 改返回排序列表或退场。候选已按 priority 排序；cluster 排序在 P2 加。

- [ ] **Step 1: 写失败测试**

`InstanceSelectorTest.java`：
- `select_returnsCandidateList`：多实例，断言返回 List 且按 priority 升序。
- `select_empty_throws`：无实例抛 ResourceNotFoundException。

- [ ] **Step 2: 跑红** → FAIL（select 返回单个）

- [ ] **Step 3: 改 InstanceSelector**

`select` 返回 `List<ModelInstance>`，去掉 `getFirst()`。LoadBalanceRouter 降级：@Order 仍 9999 但 `isForce()` 改 false，且 filter 直接返回输入列表（不再收敛）；或直接从 RouterChain 移除（grep 注入点）。

- [ ] **Step 4: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=InstanceSelectorTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(routing): InstanceSelector.select 改返回候选列表供 L1 逐个试"
```

### Task 3.2: InstanceSelector.select 返回候选列表 + RoutingResolver 适配

**Files:**
- Modify: `application/proxy/RoutingResolver.java`
- Modify: 所有 `RoutingResolver.resolve` 调用点（`DegradationInvoker`、`ChatDispatchServiceImpl`）
- Modify: `RoutingContext`（若需承载候选列表）
- Test: `RoutingResolverTest.java`

**Interfaces:**
- Produces: `RoutingResolver.resolve(...)` 返回 `RoutingContext`（含候选列表）或新增 `resolveCandidates(...)` 返回 `List<RoutingContext>`。

> 现 `RoutingContext` 是单 channel 上下文。L1 需候选列表。方案：`RoutingContext` 增 `List<RoutingContext> candidates`，或 `RoutingResolver.resolveCandidates` 返回 `List<RoutingContext>`。选后者更清晰。Read `RoutingContext.java` 确认结构。

- [ ] **Step 1: 确认 RoutingContext 结构**

Run: codegraph explore `RoutingContext record resolve`。

- [ ] **Step 2: 写失败测试**

`RoutingResolverTest.java`：`resolveCandidates` 返回按 priority 排序的多个 RoutingContext。

- [ ] **Step 3: 实现 resolveCandidates**

`RoutingResolver` 委托 `InstanceSelector.select` 拿候选列表，逐个转 RoutingContext。

- [ ] **Step 4: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=RoutingResolverTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(routing): RoutingResolver 适配候选列表输出"
```

### Task 3.3: 新增 ChannelFailoverInvoker（候选内逐个试）

**Files:**
- Create: `application/proxy/invoker/ChannelFailoverInvoker.java`
- Test: `ChannelFailoverInvokerTest.java`

**Interfaces:**
- Consumes: `KeyFailoverInvoker.invoke(ctx, request)`（L0 在内部跑）、`ErrorClassifier.classify(ProviderErrorType)`（Task 3.4）、`RoutingResolver.resolveCandidates(...)`、`DegradationService.degrade(reason)`（L2）。
- Produces:
  - `ProtocolResponse invoke(RoutingContext primaryCtx, List<RoutingContext> candidates, ProtocolRequest request, Protocol inboundProtocol, Long applicationId, ResilienceProfile profile)`
  - `void invokeStream(...同..., StreamCallback callback)`

**语义（D3/D5/深化点5）**：
1. 候选列表内逐个试（实时查熔断跳过）。
2. 捕获 ProviderException → `ErrorClassifier.classify` → L1/L2/NONE。
3. L1：换下一个候选（共因故障），L1 全耗尽才进 L2。
4. L2：`degrade(reason)` 换模型，受画像门禁（`enableL2ModelDegradation`）。
5. NONE：直接抛。
6. INVALID_REQUEST：绝不转移，直接抛。

- [ ] **Step 1: 写失败测试**

`ChannelFailoverInvokerTest.java`：
- `l1_failoverToNextCandidate`：ch1 抛 AUTH（共因），ch2 成功，断言调 ch2。
- `l1_exhausted_thenL2`：所有候选 AUTH 耗尽，degrade 返回 fallback，断言走 L2。
- `invalidRequest_noFailover`：ch1 抛 INVALID_REQUEST，断言直接抛不试 ch2。
- `streamOnlyBeforeFirstByte`：流式首字节后失败不换渠道。

- [ ] **Step 2: 跑红** → FAIL（类不存在）

- [ ] **Step 3: 实现 ChannelFailoverInvoker**

按上面语义实现，依赖 Task 3.4 ErrorClassifier。若 3.4 未完成，先建 ErrorClassifier 骨架。

- [ ] **Step 4: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChannelFailoverInvokerTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(failover): 新增 ChannelFailoverInvoker 实现 L1 候选内逐个试与 L2 降级分流"
```

### Task 3.4: 错误分流表实现

**Files:**
- Create: `application/proxy/failover/ErrorClassifier.java`
- Create: `domain/supply/enums/FailoverDecision.java`（L1/L2/NONE）
- Test: `ErrorClassifierTest.java`

**Interfaces:**
- Produces: `ErrorClassifier.classify(ProviderErrorType type) → FailoverDecision`。
- `FailoverDecision` 枚举：`L1, L2, NONE`。

**分流表（D3）**：
- `INVALID_REQUEST` → NONE（请求级错误，换哪都无效）
- 共因故障（`AUTH`/`QUOTA`/`RATE_LIMIT`/`NETWORK`/`UPSTREAM_ERROR`/`TIMEOUT`/`SERVER_ERROR`）→ L1
- 其余 → L2（模型能力问题，换模型）
> 具体 ProviderErrorType 枚举值以 `domain/supply/enums/ProviderErrorType.java` 为准，实现前 Read 确认。

- [ ] **Step 1: 确认 ProviderErrorType 枚举值**

Run: codegraph explore `ProviderErrorType enum`。

- [ ] **Step 2: 写失败测试**

`ErrorClassifierTest.java`：逐个 ProviderErrorType 断言分流结果（INVALID_REQUEST→NONE，AUTH→L1 等）。

- [ ] **Step 3: 跑红** → FAIL

- [ ] **Step 4: 实现**

`ErrorClassifier` 用 switch/Map 映射，中文注释分流理由。

- [ ] **Step 5: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ErrorClassifierTest` → PASS

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(failover): 错误分流表实现 ProviderErrorType→L1/L2/NONE"
```

### Task 3.5: 流式转移边界（首字节前）

**Files:**
- Modify: `application/proxy/invoker/ChannelFailoverInvoker.java`（invokeStream）
- Test: `ChannelFailoverInvokerTest`（流式边界场景）

**语义**：流式只在首字节前转移。首字节后失败不换渠道（继承 KeyFailoverInvoker 现有约束）。需 StreamCallback 标记首字节是否已发送。

- [ ] **Step 1: 确认 StreamCallback 结构**

Run: codegraph explore `StreamCallback onFirstByte onChunk`。

- [ ] **Step 2: 写失败测试**

`ChannelFailoverInvokerTest` 增 `stream_afterFirstByte_noFailover`：首字节已发送后 ch1 失败，断言不试 ch2，直接抛/结束。

- [ ] **Step 3: 实现 + 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChannelFailoverInvokerTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(failover): 流式转移边界限定首字节前"
```

### Task 3.6: DegradationInvoker 退场/降级为内部组件

**Files:**
- Modify: `application/proxy/invoker/DegradationInvoker.java`
- Modify: `application/proxy/ChatDispatchServiceImpl.java`（改用 ChannelFailoverInvoker）
- Test: `ChatDispatchServiceTest.java` 适配

**语义**：`DegradationInvoker` 被 `ChannelFailoverInvoker` 取代（L2 降级在 ChannelFailoverInvoker 内部）。`DegradationInvoker` 删除或降级为内部 Helper。`ChatDispatchServiceImpl` 的 12 处 DegradationInvoker 调用改 ChannelFailoverInvoker。

- [ ] **Step 1: grep DegradationInvoker 调用点**

Run: `grep -rn "DegradationInvoker" gateway-boot/src/main`

- [ ] **Step 2: ChatDispatchServiceImpl 改用 ChannelFailoverInvoker**

替换调用，构造候选列表传入。

- [ ] **Step 3: DegradationInvoker 退场**

删除类（若 ChannelFailoverInvoker 完全覆盖）或改为 package-private Helper。删除对应测试或迁移逻辑。

- [ ] **Step 4: 测试 + 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ChatDispatchServiceTest` → PASS

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "refactor(failover): DegradationInvoker 退场，ChatDispatchService 改用 ChannelFailoverInvoker"
```

### Task 3.7: P1 单元与集成测试

**Files:**
- Test: `integration/ChannelFailoverIntegrationTest.java`

- [ ] **Step 1: 写集成测试**

场景：
- L1 转移：ch1 共因故障→ch2 成功。
- 错误分流：INVALID_REQUEST 不转移。
- 流式边界：首字节前转移、后不转移。
- 跨 Cluster 不越权（P2 Cluster 落地后补，此处先占位）。
- 两对照场景：Claude Code 禁降级（profile 关 L2）/ 客服全开。

- [ ] **Step 2: P1 全段回归**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test(failover): P1 L1 转移与错误分流端到端集成测试"
```

---

## 里程碑 P2：应用级容灾画像 + Cluster 故障域分组

### Task 4.1: ResilienceProfile 实体 + 表 + Gateway

**Files:**
- Create: `domain/resilience/entity/ResilienceProfile.java`
- Create: `domain/resilience/gateway/ResilienceProfileGateway.java`
- Create: `infrastructure/resilience/gateway/ResilienceProfileGatewayImpl.java` + JPA Repository + PO
- Create: `db/migration/V40__add_resilience_profile_tables.sql`
- Test: `ResilienceProfileGatewayImplTest.java`

**Interfaces:**
- Produces: `ResilienceProfile` 字段：`Long id; String code; String name; String mode; boolean enableL2ModelDegradation; int degradationMaxDepth; boolean enableSessionAffinity; int sessionAffinityTtlMinutes; boolean enablePinnedModel; Long pinnedModelId; int timeout; ...审计字段`。Gateway: `findById/findByCode/findAll/save`。

- [ ] **Step 1: 实体 + 迁移**

建实体（@Getter @Setter）+ V40 表（含审计字段）。

- [ ] **Step 2: Gateway 接口 + 实现**

参照 ChannelGateway 模式。

- [ ] **Step 3: 测试 + 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ResilienceProfileGatewayImplTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(resilience): ResilienceProfile 实体与 resilience_profiles 表及 Gateway"
```

### Task 4.2: Cluster 实体 + 表 + Channel.cluster_id FK

**Files:**
- Create: `domain/resilience/entity/Cluster.java`
- Create: `domain/resilience/gateway/ClusterGateway.java` + Impl
- Modify: `domain/supply/entity/Channel.java`（增 clusterId）
- Create: `db/migration/V41__add_cluster_tables.sql`
- Test: `ClusterGatewayImplTest.java`

**Interfaces:**
- Produces: `Cluster` 字段 `Long id; String code; String name; Long providerId; String region; int priority; ClusterHealthStatus healthStatus; 审计`。`Channel.clusterId`。Gateway: `findById/findByCode/findAll/save`。

- [ ] **Step 1: 实体 + Channel 加字段 + 迁移**

V41 建 clusters 表 + `ALTER TABLE channels ADD COLUMN cluster_id BIGINT`。

- [ ] **Step 2: Gateway + 实现 + 测试**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ClusterGatewayImplTest` → PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(resilience): Cluster 故障域实体与 Channel.cluster_id 关联"
```

### Task 4.3: Application 挂 resilience_profile_id + 解析链 Application→Global

**Files:**
- Modify: `domain/application/entity/Application.java`（resilienceProfileId 已在 1.1 预留）
- Create: `application/resilience/ResilienceResolver.java`
- Test: `ResilienceResolverTest.java`

**Interfaces:**
- Consumes: `ApplicationGateway.findById`、`ResilienceProfileGateway.findById/findByCode`
- Produces: `ResilienceResolver.resolve(Long applicationId) → ResilienceProfile`（Application 画像为主，无则回退全局 `default` 画像）。

- [ ] **Step 1: 写失败测试**

`ResilienceResolverTest.java`：
- `applicationHasProfile_returnsAppProfile`：Application 挂 profile=5，返回 profile 5。
- `applicationNoProfile_returnsGlobalDefault`：Application 无画像，返回 code='default' 全局画像。

- [ ] **Step 2: 跑红 → 实现 → 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ResilienceResolverTest` → PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(resilience): ResilienceResolver 解析链 Application→Global"
```

### Task 4.4: 预设档位初始化数据

**Files:**
- Create: `db/migration/V42__seed_resilience_profiles.sql`
- Modify: `application/init/`（DataLoader 模式或纯 SQL）

**档位（D5）**：default/strict/aggressive/batch。

- [ ] **Step 1: 写 seed SQL**

V42 插入四个预设档位，字段值按 design D5 推导（STANDARD/STRICT/AGGRESSIVE + BATCH 为 STANDARD 的 QUEUED 变体）。

- [ ] **Step 2: 测试 seed 幂等**

Run: `./mvnw -pl gateway-boot -am test` → 全绿（seed 不重复）

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(resilience): 预设容灾档位 default/strict/aggressive/batch 初始化数据"
```

### Task 4.5: 容灾模式档位 → Profile 字段自动推导

**Files:**
- Create: `application/resilience/ResilienceProfileApplier.java`
- Test: `ResilienceProfileApplierTest.java`

**Interfaces:**
- Produces: `ResilienceProfileApplier.apply(ResilienceProfile base, String mode) → ResilienceProfile`（按档位覆盖专家字段）。

- [ ] **Step 1: 写失败测试**

`ResilienceProfileApplierTest.java`：mode=STRICT 断言 enableL2ModelDegradation=false 等；STANDARD 断言宽松值。

- [ ] **Step 2: 跑红 → 实现 → 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ResilienceProfileApplierTest` → PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(resilience): 容灾模式档位自动推导画像专家字段"
```

### Task 4.6: 会话亲和 SessionAffinityStore（Redis/InMemory 双实现）

**Files:**
- Create: `domain/resilience/gateway/SessionAffinityStore.java`
- Create: `infrastructure/resilience/affinity/RedisSessionAffinityStore.java`
- Create: `infrastructure/resilience/affinity/InMemorySessionAffinityStore.java`
- Create: `infrastructure/config/SessionAffinityConfig.java`（按 profile 选实现）
- Test: `SessionAffinityStoreTest.java`

**Interfaces:**
- Produces: `SessionAffinityStore`: `Long get(String sessionId); void put(String sessionId, Long channelId); void evict(String sessionId);`，TTL 30min。
- `@ConfigurationProperties` `session.affinity.ttl-minutes=30`、`session.affinity.enabled`。

**语义（D6/深化点6）**：X-Session-Id→channelId，TTL 30min，亲和优先非强制（熔断则转移并更新），标识缺失不亲和。

- [ ] **Step 1: 接口 + 两个实现 + 配置**

InMemory 用 ConcurrentHashMap + ScheduledExecutor 过期；Redis 用 StringRedisTemplate + expire。Config 用 `@ConditionalOnProperty` 选实现（生产 Redis，开发 InMemory）。

- [ ] **Step 2: 写测试**

`SessionAffinityStoreTest.java`（针对 InMemory）：put/get/evict、TTL 过期、标识缺失返回 null。

- [ ] **Step 3: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=SessionAffinityStoreTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(resilience): SessionAffinityStore Redis/InMemory 双实现，TTL 30min 亲和优先非强制"
```

### Task 4.7: Cluster 级健康聚合 + ClusterAffinityRouter

**Files:**
- Create: `application/proxy/routing/ClusterHealthAggregator.java`
- Create: `application/proxy/routing/ClusterAffinityRouter.java`（@Order 250，Health 后 Priority 前）
- Modify: `Cluster` 实体 healthStatus 更新逻辑
- Test: `ClusterHealthAggregatorTest.java`、`ClusterAffinityRouterTest.java`

**语义**：域内所有渠道熔断 → Cluster DOWN，路由跳过整域；域内任一渠道 half-open 成功 → 解除 DOWN。ClusterAffinityRouter 就近/按域锁定。

- [ ] **Step 1: 写失败测试**

`ClusterHealthAggregatorTest.java`：域内全熔断→DOWN；任一 half-open 成功→解除。

- [ ] **Step 2: 跑红 → 实现 → 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=ClusterHealthAggregatorTest,ClusterAffinityRouterTest` → PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(resilience): Cluster 域级健康聚合与亲和路由"
```

### Task 4.8: DegradationService.degrade(reason) 按 reason 分流，L2 受画像门禁

**Files:**
- Modify: `application/degradation/DegradationService.java` + Impl
- Modify: `ChannelFailoverInvoker`（L2 前检查 `profile.enableL2ModelDegradation`）
- Test: `DegradationServiceTest.java`

**语义（proposal Modified Capabilities）**：degrade(model, errorType) → 按 errorType 分流；L2 受 `enableL2ModelDegradation`/`degradationMaxDepth` 门禁。

- [ ] **Step 1: 写失败测试**

`DegradationServiceTest.java`：profile 关 L2 时 degrade 返回 null；开时返回 fallback；maxDepth 控制。

- [ ] **Step 2: 跑红 → 实现 → 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=DegradationServiceTest` → PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat(degradation): degrade 按 reason 分流且 L2 受画像门禁"
```

### Task 4.9: RoutingRequest 增 resilienceProfile 贯穿 + PinnedModelRouter

**Files:**
- Modify: `application/proxy/routing/RoutingRequest.java`（增 resilienceProfile）
- Create: `application/proxy/routing/PinnedModelRouter.java`（@Order 350，Priority 后）
- Modify: RouterChain 各 Router 取 profile
- Test: `PinnedModelRouterTest.java`

**Interfaces:**
- Produces: `RoutingRequest.getResilienceProfile()`；`PinnedModelRouter` 按画像 pinnedModelId 锁定。

- [ ] **Step 1: RoutingRequest 增 profile 字段**

- [ ] **Step 2: 写 PinnedModelRouter 测试 + 实现**

profile.enablePinnedModel 时只保留 pinnedModelId 实例。

- [ ] **Step 3: 跑绿**

Run: `./mvnw -pl gateway-boot -am test -Dtest=PinnedModelRouterTest` → PASS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(routing): RoutingRequest 贯穿画像并新增 PinnedModelRouter"
```

### Task 4.10: P2 单元与集成测试

**Files:**
- Test: `integration/ResilienceProfileIntegrationTest.java`、`integration/ClusterFailoverIntegrationTest.java`

- [ ] **Step 1: 写集成测试**

覆盖：解析链、档位推导、会话亲和（标识缺失不亲和、熔断转移更新）、画像继承、Cluster 健康聚合（共因隔离）、亲和路由。两对照场景端到端（Claude Code 禁降级 / 客服全开）。

- [ ] **Step 2: P2 全段回归**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test(resilience): P2 画像/Cluster/会话亲和端到端集成测试"
```

### Task 4.11: 前端画像模板页 + 容灾总览页 + Channels 应急操作

**Files:**
- Create: `gateway-console/src/pages/resilience/profiles/*`（模板 CRUD，专家字段折叠）
- Modify: Applications 页加容灾模式档位选择 + 降级兜底开关
- Create: `gateway-console/src/pages/resilience/overview/*`（故障域拓扑 + 转移事件流 + 耗尽告警）
- Modify: Channels 页一键熔断/恢复/紧切域
- 依赖：后端 ResilienceProfile/Cluster/容灾事件 Controller（P2 内补建）

- [ ] **Step 1: 补建后端 Controller**

ResilienceProfileController（CRUD）、ClusterController、ResilienceEventController（转移事件流）、Channel 应急操作端点。

- [ ] **Step 2: 前端三屏实现**

- [ ] **Step 3: 前端构建验证**

Run: `cd gateway-console && npm run build` → 通过

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(console): 容灾画像模板页/总览页/Channels 应急操作"
```

---

## 收尾

### Task 5.1: 更新 spec（移除 team-channel-management，新增相关 capability spec）

**Files:**
- Modify: `openspec/specs/`（移除 team-channel-management spec，新增 application/application-access-control/channel-failover/resilience-profile/cluster-failover/resilience-console spec delta）

- [ ] **Step 1: 编辑 delta spec**

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "docs(spec): 更新 capability spec（移除 team-channel-management，新增容灾相关）"
```

### Task 5.2: 全链路回归测试

- [ ] **Step 1: 全量测试**

Run: `./mvnw -pl gateway-boot -am test` → 全绿

- [ ] **Step 2: 前端构建**

Run: `cd gateway-console && npm run build` → 通过

- [ ] **Step 3: 两对照场景端到端确认**

Claude Code 禁降级 / 客服全开 两个 profile 端到端跑通。

- [ ] **Step 4: Commit（如有修复）**

### Task 5.3: 文档更新

**Files:**
- Modify: `doc/容灾方案设计.md`、`doc/容灾管理范式.md`（与实现对齐）

- [ ] **Step 1: 文档对齐**

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "docs: 容灾方案与管理范式文档与实现对齐"
```

---

## Self-Review

**1. Spec coverage:**
- P-r（1.1–1.9）覆盖 Application/ApplicationChannel 实体、UserApiKey 锚点、PermissionRouter 重写、拦截器、迁移、Team 移除、测试、前端。✓
- P0（2.1–2.4）覆盖 RouterChain 顺序、熔断 key 统一、ProviderHealthTracker 收窄、测试。✓
- P1（3.1–3.7）覆盖候选列表、InstanceSelector/RoutingResolver 适配、ChannelFailoverInvoker、错误分流表、流式边界、DegradationInvoker 退场、测试。✓
- P2（4.1–4.11）覆盖 ResilienceProfile、Cluster、解析链、档位、会话亲和、Cluster 健康聚合、DegradationService 分流、PinnedModelRouter、测试、前端。✓
- 收尾（5.1–5.3）覆盖 spec、回归、文档。✓
- D8（不建 ApplicationModel）已在 Global Constraints 明确，无 application_models 表任务。✓

**2. Placeholder scan:** 计划含具体 SQL/测试场景/命令，无 TBD。部分任务（如 1.7 grep 修复点、3.2 RoutingContext 结构）依赖运行时确认，已标注「实现前 Read/grep 确认」而非留空。

**3. Type consistency:** `InstanceSelector.select` 返回 `List<ModelInstance>` 在 3.1/3.2 一致；`RoutingRequest` 字段 applicationId（1.4）/resilienceProfile（4.9）逐步累加，构造器链一致；`ErrorClassifier.classify→FailoverDecision`、`ChannelFailoverInvoker.invoke` 签名在 3.3/3.4/3.6 一致。

**已知风险与执行注意:**
- D8 与 proposal 第105行 `application_models` 表冲突，已按 design D8 覆盖（不建）。执行时若 tasks.md/proposal 残留 application_models 引用，以本计划为准。
- Task 1.6 迁移字段名需先确认 teams 表结构（Read 早期 V*.sql）。
- Task 2.2/3.2 需确认 ModelInstance/RoutingContext 的 endpointId 字段，可能需子任务加字段+迁移。
- 前端任务（1.9/4.11）依赖后端 Controller，后端 Controller 未显式单列任务，在对应前端任务 Step 1 补建。

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-19-resilience-architecture.md`。

执行方式选择（由 comet-build Step 3 用户决策点确定）：
1. **Subagent-Driven**（推荐）— 每任务派发独立 subagent + 双阶段审查，适合 34 任务大体量。
2. **Inline Execution** — 当前 session 用 executing-plans 批量执行带检查点。
