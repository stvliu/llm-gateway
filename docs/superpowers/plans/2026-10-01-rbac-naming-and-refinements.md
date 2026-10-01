# RBAC 逻辑层正名与小修正实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将全系统认证与授权统一为 RBAC 语义：领域层 Javadoc/文档以 RBAC 术语正名（主体=用户、角色两域=users.role 与应用即角色、授权=角色-资源），数据面 `Identity.role` 真实化，认证侧公开路径收敛为授权侧单一事实源。

**Architecture:** 纯语义层演进，零 schema 变更：① `Application`/`ApplicationChannel`/`UserApiKey`/`AuthorizationService`/`PermissionRouter` Javadoc 以 RBAC 术语正名；② `AuthenticationService` 注入 `UserRepository`，`authenticateUser` 返回真实用户角色（现状硬编码 `"user"`；数据面透传链 `ChatDispatchServiceImpl → InstanceSelector → RoutingRequest` 的 role 无消费者，零行为风险）；③ `AuthorizationService` 新增 `publicPathPatterns()`（PUBLIC scope 规则派生），`GatewayAuthenticatorInterceptor.PUBLIC_PATHS` 改为引用该派生（单一事实源）；④ `api-spec.md` 补充 RBAC 表语义映射。

**Tech Stack:** Java 21 + Spring Boot 3.5 + JUnit 5 + Mockito + AssertJ

## Global Constraints

- **行为零变化**（role 真实化除外，已论证零行为影响：`RoutingRequest.getRole()` 全链无消费者）
- iam 域核心不得依赖 jakarta.servlet
- 物理表名**不改**（`applications`/`application_channel`/`user_api_keys` 保留——业务实体语义，逻辑层正名）
- 公开路径单一事实源：授权侧 `AuthorizationService.CONTROL_RULES` 的 `SCOPE_PUBLIC` 规则为唯一来源；认证侧 `GatewayAuthenticatorInterceptor` 派生引用（web → iam 依赖合法）
- PUBLIC scope 规则须为精确路径（无 Ant 通配符），认证侧保持精确匹配语义（Javadoc 注明约束）
- 模块坐标：`gateway-iam/iam`、`gateway-web`、`gateway-proxy/proxy`；测试命令 `mvn -q -pl <module> -am test`（`-Dtest=` 筛选时追加 `-Dsurefire.failIfNoSpecifiedTests=false`）
- 代码注释、Javadoc、commit message 一律中文（项目语言规范）
- Windows Git Bash：mvn 报 `NoDefaultCurrentDirectoryInExePath` 时用 `env -u NoDefaultCurrentDirectoryInExePath mvn ...` 重试

---

### Task 1: 领域层 Javadoc RBAC 正名（纯注释）

**Files:**
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/Application.java`（类 Javadoc）
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationChannel.java`（类 Javadoc）
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/apikey/UserApiKey.java`（类 Javadoc）
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java`（`permittedChannelIds` Javadoc）
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java`（类 Javadoc）

**Interfaces:**
- Consumes: 无（纯注释改动）
- Produces: 无接口变化。Task 2/3/4 不受影响（编译验证通过即可）

- [ ] **Step 1: 修改五个文件的类/Javadoc 注释**

逐文件修改（保留原有版权头与既有内容，在类 Javadoc 中补充/调整 RBAC 术语；注释中文）：

`Application.java` 类 Javadoc（现有 `public class Application extends BaseEntity` 上方）补充：

```java
/**
 * 应用（数据面角色实体）
 *
 * <p>在统一 RBAC 语义下，应用即「APPLICATION 类型角色」：无 owner 的全局实体，
 * 可被多个用户（经各自 API Key 绑定）共享，其授权范围由
 * {@link ApplicationChannel}（角色-渠道权限）决定。数据面授权判定
 * 以 applicationId（角色 ID）为锚点，D9 约束：无用户角色特权旁路。</p>
 */
```

`ApplicationChannel.java` 类 Javadoc 补充：

```java
/**
 * 应用-渠道关联（角色-渠道权限）
 *
 * <p>统一 RBAC 语义下为「APPLICATION 角色 → 渠道资源」的授权记录：
 * applicationId 即角色 ID，action 恒为 route（路由调用），
 * priority 为路由转移顺序（同一渠道对不同应用可有不同优先级）。
 * 数据面 {@code AuthorizationService.permittedChannelIds} 据此解析角色可见渠道集合。</p>
 */
```

`UserApiKey.java` 类 Javadoc 补充：

```java
/**
 * 用户 API Key（主体-角色绑定凭证）
 *
 * <p>统一 RBAC 语义下：userId 为认证主体（用户），applicationId 为数据面角色
 * （APPLICATION 类型）——一个 Key 绑定「主体 + 角色」，认证后 {@code Identity}
 * 同时携带两者。凭证验证为 RBAC 前置（主体解析），不属于 RBAC 构件。</p>
 */
```

`AuthorizationService.java` 的 `permittedChannelIds` Javadoc 修改为：

```java
    /**
     * 数据面授权：应用角色可见渠道集合
     *
     * <p>统一 RBAC 语义下，applicationId 即「APPLICATION 类型角色」ID——本方法
     * 按角色解析可见渠道权限（委托 {@link ApplicationChannelRepository}）。
     * D9 语义保留：无用户角色特权旁路；applicationId 为 null（无角色）返回空集。</p>
     *
     * @param applicationId 应用 ID（数据面角色锚点）
     * @return 该角色（应用）可见的渠道 ID 集合
     */
```

`PermissionRouter.java` 类 Javadoc 修改（现有文本中"数据面权限锚点为 applicationId"处补充 RBAC 术语）：

```java
/**
 * 权限路由器 — 按应用-渠道授权（ApplicationChannel）过滤模型实例
 *
 * <p>统一 RBAC 语义下，applicationId 即「APPLICATION 类型角色」ID，
 * {@link AuthorizationService#permittedChannelIds(Long)} 按角色解析可见渠道集合，
 * 本路由过滤实例到角色可见渠道内，再过滤活跃渠道。D9：无用户角色特权旁路。</p>
 */
```

- [ ] **Step 2: 编译验证**

Run: `mvn -q -pl gateway-iam/iam,gateway-proxy/proxy,gateway-web -am test -DskipTests`
Expected: BUILD SUCCESS（纯注释改动，编译通过即可）

- [ ] **Step 3: 提交**

```bash
git add gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/Application.java gateway-iam/iam/src/main/java/com/codingas/gateway/iam/application/ApplicationChannel.java gateway-iam/iam/src/main/java/com/codingas/gateway/iam/apikey/UserApiKey.java gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java
git commit -m "docs(iam): 领域层 RBAC 语义正名——应用即数据面角色、渠道授权即角色-资源授权"
```

---

### Task 2: Identity.role 真实化（AuthenticationService 查真实用户角色）

**Files:**
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthenticationService.java`
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthenticationServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository.findById(Long)`、`User.getRole()`（均存在）；`Identity` record
- Produces: `AuthenticationService` 构造器新增 `UserRepository` 依赖；`authenticateUser` 返回的 `Identity.role` 从硬编码 `"user"` 变为真实 `users.role`。下游消费方（`ChatDispatchServiceImpl` 透传 → `InstanceSelector` → `RoutingRequest.getRole()`）无消费者，零行为影响

- [ ] **Step 1: 改写失败测试（mock UserRepository）**

先读取现有 `AuthenticationServiceTest.java`（存在：`gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthenticationServiceTest.java`，测试 `authenticateUser` 的通过/失败场景）。在 `@Mock` 区新增 `UserRepository userRepository`，`setUp` 中构造 `new AuthenticationService(userApiKeyRepository, encryptionService, userRepository)`（注意现有构造器参数顺序——实现者读取现有文件后按序追加）。

新增/修改测试断言（role 真实化）：

```java
    @Test
    @DisplayName("认证成功：身份携带真实用户角色（users.role）")
    void authenticate_validKey_returnsRealRole() {
        // 现有 mock：findByKeyPrefix 返回 key，hashKey 匹配，isAvailable true
        User user = new User();
        user.setRole("ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Identity identity = authenticationService.authenticateUser("sk-test123");

        assertThat(identity.role()).isEqualTo("ADMIN");
        assertThat(identity.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("用户已删除：角色为 null（数据面不消费 role，无影响）")
    void authenticate_userDeleted_roleIsNull() {
        // 现有 mock 同前，但 findById 返回 empty
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Identity identity = authenticationService.authenticateUser("sk-test123");

        assertThat(identity.role()).isNull();
        assertThat(identity.userId()).isEqualTo(1L);
    }
```

注：现有测试中断言 `identity.role()` 为 `"user"` 的用例需同步更新为真实角色断言（实现者按上例调整）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=AuthenticationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL（构造器参数不匹配 / role 断言失败）

- [ ] **Step 3: 修改实现**

`AuthenticationService` 改造（构造器注入 + 真实角色）：

```java
    private final UserApiKeyRepository userApiKeyRepository;
    private final ApiKeyEncryptor encryptionService;
    private final UserRepository userRepository;

    public AuthenticationService(UserApiKeyRepository userApiKeyRepository,
                                 ApiKeyEncryptor encryptionService,
                                 UserRepository userRepository) {
        this.userApiKeyRepository = userApiKeyRepository;
        this.encryptionService = encryptionService;
        this.userRepository = userRepository;
    }
```

`authenticateUser` 末尾（`return Identity.of(...)` 处）替换：

```java
        // 真实用户角色（统一 RBAC 语义：主体=用户，users.role 为控制面角色域事实源）；
        // 用户已删除时角色为 null（数据面透传链无 role 消费者，无行为影响）
        String role = userRepository.findById(userApiKey.getUserId())
                .map(User::getRole)
                .orElse(null);

        return Identity.of(
                userApiKey.getUserId(),
                role,
                userApiKey.getId(),
                userApiKey.getApplicationId()
        );
```

新增 import：`com.codingas.gateway.iam.user.User`、`com.codingas.gateway.iam.user.UserRepository`。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam -am test`
Expected: PASS（AuthenticationServiceTest 全绿 + iam 模块无回归）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthenticationService.java gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthenticationServiceTest.java
git commit -m "feat(iam): Identity.role 真实化——数据面身份携带 users.role 真实角色"
```

---

### Task 3: 公开路径单一事实源（认证侧派生授权侧 PUBLIC 规则）

**Files:**
- Modify: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java`
- Modify: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthorizationServiceTest.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptor.java`
- Modify: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptorTest.java`

**Interfaces:**
- Consumes: `AuthorizationService.CONTROL_RULES`（`SCOPE_PUBLIC` 规则，现仅 `POST /api/v1/auth/login`）；`GatewayAuthenticatorInterceptor.PUBLIC_PATHS`（现 `POST /api/v1/auth/login`、`POST /api/v1/auth/logout`——注意 logout 已在上轮修复为授权侧 PUBLIC scope）
- Produces: `AuthorizationService.publicPathPatterns()` 返回 `List<String>`（PUBLIC scope 规则的 `pathPattern` 列表，唯一事实源）；`GatewayAuthenticatorInterceptor` 构造器注入 `AuthorizationService`，`PUBLIC_PATHS` 改为 `Set.copyOf(authorizationService.publicPathPatterns())` 派生

- [ ] **Step 1: 改写失败测试**

`AuthorizationServiceTest` 新增：

```java
    @Test
    @DisplayName("publicPathPatterns：返回 PUBLIC scope 规则路径（单一事实源）")
    void publicPathPatterns_returnsPublicRules() {
        assertThat(service.publicPathPatterns()).containsExactly("/api/v1/auth/login");
    }
```

`GatewayAuthenticatorInterceptorTest` 改写（现测试直接构造 `new GatewayAuthenticatorInterceptor(authenticationService, sessionAuthenticationService)`，构造器新增 `authorizationService` 参数；`@Mock AuthorizationService authorizationService`；`@BeforeEach` stub `when(authorizationService.publicPathPatterns()).thenReturn(List.of("/api/v1/auth/login", "/api/v1/auth/logout"))`——与现 PUBLIC_PATHS 两条一致，现有「公开路径放行」用例行为不变）。

注意：现有「公开路径（auth/login、auth/logout）放行不认证」用例保持语义（mock 派生列表含两条）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam,gateway-web -am test -Dtest=AuthorizationServiceTest,GatewayAuthenticatorInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL（构造器参数不匹配 / 方法不存在）

- [ ] **Step 3: 修改实现**

`AuthorizationService` 新增方法（`CONTROL_RULES` 定义之后）：

```java
    /**
     * 公开路径列表（单一事实源）
     *
     * <p>返回 {@link SCOPE_PUBLIC} 规则的路径模式——认证侧（GatewayAuthenticatorInterceptor）
     * 的公开路径由此派生，避免双事实源漂移。约束：PUBLIC 规则必须为精确路径
     * （无 Ant 通配符），认证侧按精确匹配消费。</p>
     *
     * @return PUBLIC scope 规则的 pathPattern 列表
     */
    public List<String> publicPathPatterns() {
        return CONTROL_RULES.stream()
                .filter(rule -> SCOPE_PUBLIC.equals(rule.scope()))
                .map(ControlPermissionRule::pathPattern)
                .toList();
    }
```

新增 import：`java.util.List`（若未引入）。

`GatewayAuthenticatorInterceptor` 改造：

```java
    /** 公开路径（无需认证）——由授权侧 PUBLIC 规则派生（单一事实源） */
    private final Set<String> publicPaths;

    private final AuthenticationService authenticationService;
    private final SessionAuthenticationService sessionAuthenticationService;
    private final AuthorizationService authorizationService;

    public GatewayAuthenticatorInterceptor(AuthenticationService authenticationService,
                                           SessionAuthenticationService sessionAuthenticationService,
                                           AuthorizationService authorizationService) {
        this.authenticationService = authenticationService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        this.authorizationService = authorizationService;
        this.publicPaths = Set.copyOf(authorizationService.publicPathPatterns());
    }
```

`preHandle` 中管理路径分支（现 `PUBLIC_PATHS.contains(path)`）替换为 `publicPaths.contains(path)`；删除静态 `PUBLIC_PATHS` 常量；新增 import `com.codingas.gateway.iam.auth.AuthorizationService`、`java.util.Set`。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam,gateway-web -am test`
Expected: PASS（AuthorizationServiceTest + GatewayAuthenticatorInterceptorTest 全绿 + 两模块无回归）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthorizationServiceTest.java gateway-web/src/main/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptor.java gateway-web/src/test/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptorTest.java
git commit -m "refactor(auth): 公开路径单一事实源——认证侧派生授权侧 PUBLIC 规则"
```

---

### Task 4: 文档 RBAC 表语义映射 + 全量回归

**Files:**
- Modify: `docs/api-spec.md`（RBAC 表语义映射小节）

- [ ] **Step 1: 全量回归测试**

Run: `mvn -q test`（Windows 若报 `NoDefaultCurrentDirectoryInExePath`，用 `env -u NoDefaultCurrentDirectoryInExePath mvn -q test`）
Expected: 全模块 PASS

- [ ] **Step 2: 更新 api-spec.md**

在「统一授权模型（资源-动作-范围，v1.x）」小节之后补充：

```markdown
### RBAC 表语义映射（v1.x）

现有表结构即 RBAC 三元素（物理表名保留，语义正名）：

| 表 | RBAC 构件 | 说明 |
|----|-----------|------|
| `users` | 主体 | 用户；`role` 字段为控制面角色域（USER/ADMIN，单一事实源 `users.role`） |
| `user_api_keys` | 主体-角色绑定凭证 | `user_id`=主体（用户）、`application_id`=数据面角色；凭证验证为 RBAC 前置（主体解析） |
| `applications` | 数据面角色（APPLICATION 类型） | 无 owner 的全局实体，多用户可共享；业务属性（timeout/开通等）随实体保留 |
| `application_channel` | 角色-渠道权限 | action 恒为 route（路由调用），`priority` 为路由转移顺序 |
| `channels` / `model_instances` | 资源 | 数据面权限对象 |

控制面角色域（`users.role`）与数据面角色域（应用即角色）并存，D9 约束：数据面无用户角色特权旁路。
物理表名不做 RBAC 化重命名（业务实体语义，避免名实不符）；授权模型表化（阶段 3）时以新表形式自然 RBAC 化。
```

- [ ] **Step 3: 提交**

```bash
git add docs/api-spec.md
git commit -m "docs: 补充 RBAC 表语义映射（主体/角色两域/角色-资源授权）"
```

---

## Self-Review 记录

**1. Spec 覆盖**（对照评估结论）：
- RBAC 逻辑层正名（Javadoc/文档术语）✔ Task 1 + Task 4
- `Identity.role` 真实化 ✔ Task 2（已验证：数据面透传链 `ChatDispatchServiceImpl → InstanceSelector → RoutingRequest.getRole()` 无消费者，零行为风险）
- PUBLIC 单一事实源 ✔ Task 3（认证侧派生授权侧；web → iam 依赖合法；约束：PUBLIC 规则精确路径）
- 物理表名不动 ✔ Global Constraints（业务实体语义保留）

**2. 占位符扫描**：无 TBD/TODO；Task 2/3 测试中「现有 mock 同前」「实现者按上例调整」为对现有测试文件的适配说明（实现者需先读现有文件），核心断言已完整给出。

**3. 类型一致性**：
- `AuthorizationService.publicPathPatterns()` → `List<String>`，`GatewayAuthenticatorInterceptor` 消费为 `Set.copyOf` ✔
- `AuthenticationService` 构造器三参（userApiKeyRepository, encryptionService, userRepository）——Task 2 定义，无下游构造器依赖（Spring 自动装配）✔
- `Identity.role` 从 `String` 硬编码变真实角色，record 签名不变，下游 `identity.role()` 调用不受影响 ✔

**已知边界（刻意保留）**：
- `assignRoles`（UserController 角色分配）语义不改变（单字段写入 `roleCodes.get(0)`）——多角色化属深度 1，无需求驱动
- 服务层 `StpUtil` owner check 兜底不动（阶段 2 范围外）
- 认证机制（API Key/会话）不并入 RBAC（凭证机制语义不同）；RBAC 覆盖授权 + 主体解析
