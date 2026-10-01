# 统一授权模型（资源-动作-范围）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于"资源-动作-范围"统一授权模型，新增 `AuthorizationService` 门面：控制面规则代码化为 (资源, 动作, 范围) 权限表并下沉 iam 域，数据面渠道授权通过门面统一入口暴露，两面共用同一授权服务。

**Architecture:** iam 域新增 `AuthorizationService`（控制面 `checkControl(identity, method, path)` 判定 + 数据面 `permittedChannelIds(applicationId)` 委托）。控制面规则以 `ControlPermissionRule(resource, action, scope, method, pathPattern)` 代码化表达（与 `PermissionInterceptor` 现有四个静态规则列表语义逐条对应，零行为变化）。`PermissionInterceptor` 退化为门面调用者（移除静态规则与 `StpUtil` 角色判断）；`PermissionRouter` 改经门面取应用可见渠道。范围语义：PUBLIC（无需登录）/ LOGIN_ONLY（登录即可）/ USER（USER 或 ADMIN）/ 默认拒绝（仅 ADMIN）。

**Tech Stack:** Java 21 + Spring Boot 3.5 + AntPathMatcher（spring-core）+ JUnit 5 + Mockito + AssertJ

## Global Constraints

- 授权语义与现状**完全一致**（零行为变化）：PUBLIC（`POST /api/v1/auth/login`）、LOGIN_ONLY（logout/auth/me/me/api-keys/protocols）、USER 白名单（models/applications/experience/user-api-keys 现有规则）、ADMIN 全通、未匹配规则默认拒绝（仅 ADMIN 放行）
- iam 域核心不得依赖 jakarta.servlet（`AuthorizationService` 只处理字符串路径与方法名）
- 数据面 D9 保留：`permittedChannelIds(null)` 返回空集；无角色特权旁路；`PermissionRouter` 过滤语义不变
- 规则表**代码化、不落 DB**（阶段 1）；scope 语义不引入 DB 表达式
- `RolePermissions` 权限码（前端显隐）保持不变，`AuthorizationService` 规则与 `USER_PERMISSIONS` 语义对齐（注释说明）
- 模块坐标：`gateway-iam/iam`、`gateway-web`、`gateway-proxy/proxy`；测试命令 `mvn -q -pl <module> -am test`（`-Dtest=` 筛选时追加 `-Dsurefire.failIfNoSpecifiedTests=false`）
- 代码注释、Javadoc、commit message 一律中文（项目语言规范）
- Windows Git Bash：mvn 报 `NoDefaultCurrentDirectoryInExePath` 时用 `env -u NoDefaultCurrentDirectoryInExePath mvn ...` 重试

---

### Task 1: AuthorizationService（iam 域统一授权门面）

**Files:**
- Create: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/ControlPermissionRule.java`
- Create: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java`
- Test: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthorizationServiceTest.java`

**Interfaces:**
- Consumes: `Identity`（record，userId/role/credentialId/applicationId）、`RolePermissions.ROLE_ADMIN`/`ROLE_USER`（值 `"ADMIN"`/`"USER"`）、`ApplicationChannelRepository.findChannelIdsByApplicationId(Long)`、`org.springframework.util.AntPathMatcher`（spring-core，iam 已传递依赖）
- Produces: `ControlPermissionRule(String resource, String action, String scope, String method, String pathPattern)` record（scope 常量 `SCOPE_PUBLIC`/`SCOPE_LOGIN_ONLY`/`SCOPE_USER`）；`AuthorizationService`：`boolean checkControl(Identity, String method, String path)`、`Set<Long> permittedChannelIds(Long applicationId)`。Task 2 消费 `checkControl`，Task 3 消费 `permittedChannelIds`。

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService（统一授权门面）测试")
class AuthorizationServiceTest {

    @Mock
    private ApplicationChannelRepository applicationChannelRepository;

    private AuthorizationService service;

    /** 管理面身份（真实角色，来自 users.role） */
    private Identity identity(String role) {
        return Identity.of(1L, role, null, null);
    }

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(applicationChannelRepository);
    }

    // ---------- 控制面：PUBLIC ----------

    @Test
    @DisplayName("PUBLIC 规则（auth/login）：未登录也放行")
    void publicRule_login_passesWithoutIdentity() {
        assertThat(service.checkControl(null, "POST", "/api/v1/auth/login")).isTrue();
    }

    // ---------- 控制面：LOGIN_ONLY ----------

    @Test
    @DisplayName("LOGIN_ONLY 规则（me/api-keys）：登录放行，未登录拒绝")
    void loginOnlyRule_loggedIn_passes() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/me/api-keys")).isTrue();
        assertThat(service.checkControl(null, "GET", "/api/v1/me/api-keys")).isFalse();
    }

    @Test
    @DisplayName("LOGIN_ONLY 规则（auth/logout、auth/me、protocols）")
    void loginOnlyRules_allVariants() {
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/auth/logout")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/auth/me")).isTrue();
        assertThat(service.checkControl(identity("USER"), "PATCH", "/api/v1/auth/me/password")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/protocols")).isTrue();
    }

    // ---------- 控制面：USER 白名单 ----------

    @Test
    @DisplayName("USER 白名单（models 读/experience/applications 读）：USER 与 ADMIN 均放行")
    void userRules_userAndAdmin_pass() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/models")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/applications")).isTrue();
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/experience/chat")).isTrue();
        assertThat(service.checkControl(identity("ADMIN"), "GET", "/api/v1/models")).isTrue();
    }

    @Test
    @DisplayName("USER 白名单（user-api-keys 单条与写操作）")
    void userRules_apiKeyVariants() {
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/user-api-keys/1")).isTrue();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/user-api-keys/1/detail")).isTrue();
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/user-api-keys")).isTrue();
        assertThat(service.checkControl(identity("USER"), "PUT", "/api/v1/user-api-keys/1")).isTrue();
        assertThat(service.checkControl(identity("USER"), "DELETE", "/api/v1/user-api-keys/1")).isTrue();
    }

    @Test
    @DisplayName("USER 白名单路径：未登录与未知角色拒绝")
    void userRules_notLoggedIn_orUnknownRole_rejected() {
        assertThat(service.checkControl(null, "GET", "/api/v1/models")).isFalse();
        assertThat(service.checkControl(identity("OTHER"), "GET", "/api/v1/models")).isFalse();
    }

    // ---------- 控制面：默认拒绝（仅 ADMIN） ----------

    @Test
    @DisplayName("未匹配规则的管理路径：ADMIN 放行，USER 与未登录拒绝")
    void unmatchedManagedPath_adminOnly() {
        assertThat(service.checkControl(identity("ADMIN"), "DELETE", "/api/v1/users/1")).isTrue();
        assertThat(service.checkControl(identity("ADMIN"), "GET", "/api/v1/channels")).isTrue();
        assertThat(service.checkControl(identity("USER"), "DELETE", "/api/v1/users/1")).isFalse();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/channels")).isFalse();
        assertThat(service.checkControl(null, "GET", "/api/v1/channels")).isFalse();
    }

    @Test
    @DisplayName("USER 白名单外管理端点（providers 写/stats）拒绝 USER")
    void unmatchedManagedPath_otherEndpoints() {
        assertThat(service.checkControl(identity("USER"), "POST", "/api/v1/providers")).isFalse();
        assertThat(service.checkControl(identity("USER"), "GET", "/api/v1/stats")).isFalse();
    }

    // ---------- 数据面：permittedChannelIds ----------

    @Test
    @DisplayName("数据面：应用可见渠道集合委托查询")
    void permittedChannelIds_delegates() {
        when(applicationChannelRepository.findChannelIdsByApplicationId(10L)).thenReturn(Set.of(1L, 2L));

        assertThat(service.permittedChannelIds(10L)).containsExactlyInAnyOrder(1L, 2L);
        verify(applicationChannelRepository).findChannelIdsByApplicationId(10L);
    }

    @Test
    @DisplayName("数据面：applicationId 为 null 返回空集（D9：无权限锚点）")
    void permittedChannelIds_nullApplicationId_returnsEmpty() {
        assertThat(service.permittedChannelIds(null)).isEmpty();
        verifyNoInteractions(applicationChannelRepository);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=AuthorizationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败 `cannot find symbol: class AuthorizationService`

- [ ] **Step 3: 最小实现**

`ControlPermissionRule.java`：

```java
package com.codingas.gateway.iam.auth;

/**
 * 控制面授权规则（代码化权限表）
 *
 * <p>统一授权模型的「资源-动作-范围」元组在控制面的落地：
 * {@code resource}/{@code action} 为语义标签（与 {@link RolePermissions} 权限码对齐，
 * 供审计与未来表化使用），{@code scope} 为授权级别，{@code method}/{@code pathPattern}
 * 为判定输入（Ant 风格路径模式）。</p>
 *
 * @param resource    资源类型（如 model、apikey、me）
 * @param action      动作（如 read、write、execute、login、logout）
 * @param scope       授权级别（见 {@link AuthorizationService} 常量）
 * @param method      HTTP 方法（如 GET、POST）
 * @param pathPattern Ant 风格路径模式（如 /api/v1/models/**）
 */
public record ControlPermissionRule(
        String resource,
        String action,
        String scope,
        String method,
        String pathPattern) {
}
```

`AuthorizationService.java`：

```java
package com.codingas.gateway.iam.auth;

import com.codingas.gateway.iam.application.ApplicationChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

/**
 * 统一授权门面
 *
 * <p>以「资源-动作-范围」统一模型承载两面授权判定：</p>
 * <ul>
 *   <li><b>控制面</b>（{@link #checkControl}）：管理 API 的功能级授权。
 *       规则代码化于 {@link #CONTROL_RULES}（与前端 {@link RolePermissions} 权限码语义对齐），
 *       scope 语义：PUBLIC 无需登录 / LOGIN_ONLY 登录即可 / USER 需 USER 或 ADMIN 角色 /
 *       未匹配规则默认拒绝（仅 ADMIN 放行）；</li>
 *   <li><b>数据面</b>（{@link #permittedChannelIds}）：应用-渠道对象级授权，
 *       委托 {@link ApplicationChannelRepository}，D9 语义保留（无角色特权旁路，
 *       applicationId 为 null 返回空集）。</li>
 * </ul>
 */
@Service
public class AuthorizationService {

    /** scope：无需登录（认证端点本身） */
    public static final String SCOPE_PUBLIC = "PUBLIC";
    /** scope：登录即可，不限角色 */
    public static final String SCOPE_LOGIN_ONLY = "LOGIN_ONLY";
    /** scope：USER 或 ADMIN 角色 */
    public static final String SCOPE_USER = "USER";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * 控制面授权规则表（代码化权限表，阶段 1 不落 DB）
     *
     * <p>与历史 PermissionInterceptor 四个静态规则列表逐条对应，语义零变化。
     * resource/action 与 {@link RolePermissions} USER 权限码对齐
     * （dashboard、model:read、quickstart:access、key:read、key:write、application:read）。</p>
     */
    static final List<ControlPermissionRule> CONTROL_RULES = List.of(
            // PUBLIC：无需登录
            new ControlPermissionRule("auth", "login", SCOPE_PUBLIC, "POST", "/api/v1/auth/login"),
            // LOGIN_ONLY：登录即可（个人认证与只读能力）
            new ControlPermissionRule("auth", "logout", SCOPE_LOGIN_ONLY, "POST", "/api/v1/auth/logout"),
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/auth/me"),
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "PATCH", "/api/v1/auth/me/password"),
            new ControlPermissionRule("me", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/me/**"),
            new ControlPermissionRule("protocol", "read", SCOPE_LOGIN_ONLY, "GET", "/api/v1/protocols"),
            // USER 白名单：USER 或 ADMIN
            new ControlPermissionRule("model", "read", SCOPE_USER, "GET", "/api/v1/models/**"),
            new ControlPermissionRule("application", "read", SCOPE_USER, "GET", "/api/v1/applications/**"),
            new ControlPermissionRule("experience", "execute", SCOPE_USER, "POST", "/api/v1/experience/**"),
            new ControlPermissionRule("experience", "read", SCOPE_USER, "GET", "/api/v1/experience/**"),
            new ControlPermissionRule("apikey", "read", SCOPE_USER, "GET", "/api/v1/user-api-keys/*"),
            new ControlPermissionRule("apikey", "read", SCOPE_USER, "GET", "/api/v1/user-api-keys/*/*"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "POST", "/api/v1/user-api-keys"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "PUT", "/api/v1/user-api-keys/*"),
            new ControlPermissionRule("apikey", "write", SCOPE_USER, "DELETE", "/api/v1/user-api-keys/*")
    );

    private final ApplicationChannelRepository applicationChannelRepository;

    public AuthorizationService(ApplicationChannelRepository applicationChannelRepository) {
        this.applicationChannelRepository = applicationChannelRepository;
    }

    /**
     * 控制面授权判定（管理 API）
     *
     * <p>按方法 + 路径匹配规则表，按 scope 判定；未匹配规则默认拒绝（仅 ADMIN 放行）。
     * 调用方负责仅在管理路径（/api/v1/）下调用。</p>
     *
     * @param identity 认证身份（可为 null，表示未认证；PUBLIC 规则仍放行）
     * @param method   HTTP 方法
     * @param path     请求路径
     * @return true 允许；false 拒绝（调用方响应 403）
     */
    public boolean checkControl(Identity identity, String method, String path) {
        for (ControlPermissionRule rule : CONTROL_RULES) {
            if (!rule.method().equals(method)) {
                continue;
            }
            if (!MATCHER.match(rule.pathPattern(), path)) {
                continue;
            }
            return switch (rule.scope()) {
                case SCOPE_PUBLIC -> true;
                case SCOPE_LOGIN_ONLY -> identity != null;
                case SCOPE_USER -> identity != null
                        && (RolePermissions.ROLE_USER.equals(identity.role())
                        || RolePermissions.ROLE_ADMIN.equals(identity.role()));
                default -> false;
            };
        }
        // 未匹配规则的管理路径：默认拒绝，仅 ADMIN 放行
        return identity != null && RolePermissions.ROLE_ADMIN.equals(identity.role());
    }

    /**
     * 数据面授权：应用可见渠道集合
     *
     * <p>D9 语义保留：无角色特权旁路；applicationId 为 null（无权限锚点）返回空集。</p>
     *
     * @param applicationId 应用 ID（数据面权限锚点）
     * @return 应用可见的渠道 ID 集合
     */
    public Set<Long> permittedChannelIds(Long applicationId) {
        if (applicationId == null) {
            return Set.of();
        }
        return applicationChannelRepository.findChannelIdsByApplicationId(applicationId);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=AuthorizationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（11 个测试全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/ControlPermissionRule.java gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/AuthorizationService.java gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/AuthorizationServiceTest.java
git commit -m "feat(iam): 新增统一授权门面 AuthorizationService（控制面规则代码化 + 数据面渠道委托）"
```

---

### Task 2: PermissionInterceptor 改调用授权门面

**Files:**
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/PermissionInterceptor.java`
- Modify: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/PermissionInterceptorTest.java`

**Interfaces:**
- Consumes: Task 1 的 `AuthorizationService.checkControl(Identity, String, String)`；`identity` request attribute（`Identity`，统一认证拦截器注入）；`AuthorizationService` Spring Bean
- Produces: `PermissionInterceptor` 保留（name=`Permission`、order=3、`@Component`），构造器注入 `AuthorizationService`；移除静态规则列表、`AntPathMatcher`、`StpUtil`/`RolePermissions` 引用。Task 3/4 依赖其行为不变。

- [ ] **Step 1: 改写失败测试（mock AuthorizationService）**

关键改造点：原测试直接构造 `new PermissionInterceptor()` 并 stub `request.getAttribute("identity")`；新测试改为 mock `AuthorizationService`（`when(authService.checkControl(...)).thenReturn(...)`），并按需 stub identity。

```java
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.iam.auth.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionInterceptor} 角色级授权测试（授权判定委托统一门面）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionInterceptor（角色级授权）测试")
class PermissionInterceptorTest {

    @Mock
    private AuthorizationService authorizationService;

    private PermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PermissionInterceptor(authorizationService);
    }

    /** 管理路径请求：带身份（identity attribute 由统一认证拦截器注入） */
    private HttpServletRequest request(String method, String uri, Identity identity) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        lenient().when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        if (identity != null) {
            when(req.getAttribute("identity")).thenReturn(identity);
        }
        return req;
    }

    private HttpServletResponse response() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        lenient().when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return resp;
    }

    @Test
    @DisplayName("管理路径：门面判定通过则放行")
    void managedPath_authorized_passes() throws Exception {
        Identity identity = Identity.of(1L, "USER", null, null);
        when(authorizationService.checkControl(identity, "GET", "/api/v1/models")).thenReturn(true);

        assertThat(interceptor.preHandle(request("GET", "/api/v1/models", identity), response())).isTrue();
    }

    @Test
    @DisplayName("管理路径：门面判定拒绝 → 403")
    void managedPath_denied_rejected403() throws Exception {
        Identity identity = Identity.of(1L, "USER", null, null);
        when(authorizationService.checkControl(identity, "DELETE", "/api/v1/users/1")).thenReturn(false);
        HttpServletResponse resp = response();

        assertThat(interceptor.preHandle(request("DELETE", "/api/v1/users/1", identity), resp)).isFalse();
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("管理路径：身份缺失（绕过认证）→ 门面以 null 身份判定，拒绝 403")
    void managedPath_missingIdentity_rejected403() throws Exception {
        when(authorizationService.checkControl(isNull(), eq("GET"), eq("/api/v1/models"))).thenReturn(false);
        HttpServletResponse resp = response();

        assertThat(interceptor.preHandle(request("GET", "/api/v1/models", null), resp)).isFalse();
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("API Key 网关路径（/v1/）跳过授权")
    void apiKeyPath_skips() throws Exception {
        assertThat(interceptor.preHandle(request("POST", "/v1/chat/completions", null), response())).isTrue();
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("非管理路径（静态资源等）跳过")
    void nonManagedPath_skips() throws Exception {
        assertThat(interceptor.preHandle(request("GET", "/assets/app.js", null), response())).isTrue();
        verifyNoInteractions(authorizationService);
    }
}
```

注意：SSE 异步分发跳过逻辑（`DispatcherType.ASYNC`）在实现中保留，测试沿用原用例形态（mock `request.getDispatcherType()` 返回 ASYNC → 放行且不触门面）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-web -am test -Dtest=PermissionInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（构造器参数不匹配）

- [ ] **Step 3: 修改实现**

`PermissionInterceptor` 改造（删除静态规则列表与角色判断，改为门面调用）：

```java
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.iam.auth.Identity;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 角色授权拦截器
 *
 * <p>授权判定委托统一授权门面 {@link AuthorizationService}（规则代码化于 iam 域，
 * 与前端 {@code RolePermissions} 权限码语义对齐）：</p>
 * <ul>
 *   <li><b>管理路径</b>（/api/v1/）：门面按「资源-动作-范围」判定，拒绝时 403；</li>
 *   <li><b>API Key 网关路径</b>（/v1/）与非管理路径：跳过授权；</li>
 *   <li><b>SSE 异步分发</b>：跳过（授权已在初始请求校验）。</li>
 * </ul>
 */
@Slf4j
@Component
public class PermissionInterceptor extends AbstractGatewayInterceptor {

    /** 管理 API 路径前缀（需授权校验） */
    private static final String MANAGED_PREFIX = "/api/v1/";

    /** API Key 认证路径前缀（网关代理端点，由 ApiKeyAuth 认证、数据面授权处理） */
    private static final String API_KEY_PREFIX = "/v1/";

    private final AuthorizationService authorizationService;

    public PermissionInterceptor(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public String name() {
        return "Permission";
    }

    @Override
    public int order() {
        return 3; // 在 Authenticator(order=1) 之后执行
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // SSE 异步分发阶段跳过（授权已在初始请求校验）
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }

        String uri = request.getRequestURI();

        // API Key 网关路径（/v1/...），由数据面授权处理
        if (uri.startsWith(API_KEY_PREFIX)) {
            return true;
        }

        // 非管理 API 路径（静态资源等），跳过
        if (!uri.startsWith(MANAGED_PREFIX)) {
            return true;
        }

        // 授权判定：统一门面（身份可能为 null——未登录访问管理路径，门面按 PUBLIC/默认拒绝裁决）
        Identity identity = (Identity) request.getAttribute("identity");
        if (authorizationService.checkControl(identity, request.getMethod(), uri)) {
            return true;
        }

        log.warn("角色权限不足: {} {} 被拒绝", request.getMethod(), uri);
        return rejectForbidden(response, "无访问权限");
    }

    /**
     * 返回 403 并短路
     */
    private boolean rejectForbidden(HttpServletResponse response, String message) {
        try {
            reject(response, message);
        } catch (Exception e) {
            log.error("Failed to write forbidden response", e);
        }
        return false;
    }
}
```

同时：删除 `RolePermissions`、`AntPathMatcher`、`PUBLIC_RULES`/`LOGIN_ONLY_RULES`/`USER_ALLOWED_RULES`/`MATCHER` 及 `matches(...)` 方法；删除测试中全部 `mockStatic` 形态（本测试已不再用 `StpUtil`）。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-web -am test -Dtest=PermissionInterceptorTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（5 个测试全绿 + async 用例）

- [ ] **Step 5: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/interceptor/PermissionInterceptor.java gateway-web/src/test/java/com/codingas/gateway/web/interceptor/PermissionInterceptorTest.java
git commit -m "refactor(web): PermissionInterceptor 授权判定委托统一门面，移除静态规则表"
```

---

### Task 3: PermissionRouter 改经门面取应用可见渠道

**Files:**
- Modify: `gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java`
- Modify: `gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/PermissionRouterTest.java`

**Interfaces:**
- Consumes: Task 1 的 `AuthorizationService.permittedChannelIds(Long)`；现有 `ChannelRepository`
- Produces: `PermissionRouter` 构造器注入 `AuthorizationService`（替换 `ApplicationChannelRepository`），行为不变：`applicationId` null → 空集；过滤 + 活跃渠道过滤语义保留

- [ ] **Step 1: 改写失败测试（mock AuthorizationService 替换 ApplicationChannelRepository）**

关键点（实测原文件确认）：`Channel.state` 字段类型为 `ChannelState` 枚举（非 `Channel.State`）；原测试用**真实对象构造**（`new ModelInstance()`/`new Channel()` + setter）；`RoutingRequest` 用 5 参构造器 `(modelId, applicationId, userId, role, strategy)`。

```java
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.model.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionRouter} 权限路由测试（授权查询委托统一门面）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionRouter（数据面权限路由）测试")
class PermissionRouterTest {

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private AuthorizationService authorizationService;

    private PermissionRouter router;

    @BeforeEach
    void setUp() {
        router = new PermissionRouter(channelRepository, authorizationService);
    }

    private ModelInstance instance(long id, long channelId) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(channelId);
        return mi;
    }

    private Channel activeChannel(long id) {
        Channel ch = new Channel();
        ch.setId(id);
        ch.setState(ChannelState.ACTIVE);
        return ch;
    }

    private RoutingRequest request(Long applicationId) {
        return new RoutingRequest(1L, applicationId, 1L, "USER", RoutingStrategy.WEIGHTED);
    }

    @Test
    @DisplayName("应用可见渠道内的实例通过过滤")
    void filter_permittedChannelInstances_passed() {
        ModelInstance mi1 = instance(1L, 10L);
        ModelInstance mi2 = instance(2L, 20L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L, 20L));
        when(channelRepository.findByIds(List.of(10L, 20L)))
                .thenReturn(List.of(activeChannel(10L), activeChannel(20L)));

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request(100L));

        assertThat(result).containsExactly(mi1, mi2);
    }

    @Test
    @DisplayName("非授权渠道实例被过滤")
    void filter_unauthorizedChannelInstance_filtered() {
        ModelInstance mi1 = instance(1L, 10L);
        ModelInstance mi2 = instance(2L, 30L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L));
        when(channelRepository.findByIds(List.of(10L))).thenReturn(List.of(activeChannel(10L)));

        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request(100L));

        assertThat(result).containsExactly(mi1);
    }

    @Test
    @DisplayName("无权限锚点（applicationId null）返回空集，不查门面")
    void filter_nullApplicationId_empty() {
        assertThat(router.filter(List.of(instance(1L, 10L)), request(null))).isEmpty();
        verifyNoInteractions(authorizationService);
    }

    @Test
    @DisplayName("非活跃渠道的实例被过滤")
    void filter_inactiveChannelInstances_filtered() {
        ModelInstance mi1 = instance(1L, 10L);
        when(authorizationService.permittedChannelIds(100L)).thenReturn(Set.of(10L));
        Channel inactive = new Channel();
        inactive.setId(10L);
        inactive.setState(ChannelState.INACTIVE);
        when(channelRepository.findByIds(List.of(10L))).thenReturn(List.of(inactive));

        assertThat(router.filter(List.of(mi1), request(100L))).isEmpty();
    }
}
```

注：核心断言与场景（授权渠道过滤、null 锚点空集、活跃渠道过滤）与原 PermissionRouterTest 一致；若原测试存在额外场景（如实例为 null 的防御），实现者保留并仅在替换 mock 依赖时同步调整。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-proxy/proxy -am test -Dtest=PermissionRouterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: 编译失败（构造器参数不匹配）

- [ ] **Step 3: 修改实现**

`PermissionRouter` 改造（构造器与查询委托）：

```java
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.channel.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限路由器 — 按应用-渠道授权（ApplicationChannel）过滤模型实例
 *
 * <p>数据面权限锚点为 {@link RoutingRequest#getApplicationId()}：
 * 经统一授权门面 {@link AuthorizationService#permittedChannelIds(Long)} 查询应用可见渠道集合，
 * 仅保留该集合内的实例，再过滤出活跃（{@code state.isRoutable()}）渠道。</p>
 *
 * <p>D9 约束：ADMIN 退管理面，数据面权限路由无特权旁路 —— 任何角色都按应用授权过滤，
 * 不再保留 ADMIN 跳过分支。{@code applicationId} 为 null（无权限锚点）时直接返回空集。</p>
 */
@Component
@Order(100)
@RequiredArgsConstructor
public class PermissionRouter implements Router {

    private final ChannelRepository channelRepository;
    private final AuthorizationService authorizationService;

    @Override
    public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
        // 获取应用可见的渠道 ID 集合（统一授权门面：数据面授权入口）
        Set<Long> permittedChannelIds = getPermittedChannelIds(request);

        if (permittedChannelIds.isEmpty()) {
            return List.of();
        }

        // 过滤：只保留应用授权渠道内的实例
        List<ModelInstance> permitted = instances.stream()
                .filter(mi -> permittedChannelIds.contains(mi.getChannelId()))
                .toList();

        if (permitted.isEmpty()) {
            return List.of();
        }

        // 再过滤活跃 Channel（state.isRoutable()）
        List<Long> channelIds = permitted.stream().map(ModelInstance::getChannelId).toList();
        List<Channel> activeChannels = channelRepository.findByIds(channelIds).stream()
                .filter(ch -> ch.getState() != null && ch.getState().isRoutable())
                .toList();
        Set<Long> activeChannelIds = activeChannels.stream().map(Channel::getId).collect(Collectors.toSet());

        return permitted.stream()
                .filter(mi -> activeChannelIds.contains(mi.getChannelId()))
                .toList();
    }

    @Override
    public boolean isForce() { return true; }

    /**
     * 计算应用可见的渠道 ID 集合（经统一授权门面）
     *
     * <p>无权限锚点（applicationId 为 null）时返回空集；ADMIN 角色不跳过此过滤。</p>
     *
     * @param request 路由请求上下文
     * @return 应用可见的渠道 ID 集合
     */
    private Set<Long> getPermittedChannelIds(RoutingRequest request) {
        return authorizationService.permittedChannelIds(request.getApplicationId());
    }
}
```

同时：删除 `ApplicationChannelRepository` 的 import 与构造器依赖（替换为 `AuthorizationService`）。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-proxy/proxy -am test -Dtest=PermissionRouterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS（4 个测试全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-proxy/proxy/src/main/java/com/codingas/gateway/proxy/routing/PermissionRouter.java gateway-proxy/proxy/src/test/java/com/codingas/gateway/proxy/routing/PermissionRouterTest.java
git commit -m "refactor(proxy): PermissionRouter 数据面授权查询改经统一门面"
```

---

### Task 4: 全量回归与文档对齐

**Files:**
- Modify: `docs/api-spec.md`（授权章节补充统一授权模型说明）

- [ ] **Step 1: 全量回归测试**

Run: `mvn -q test`（Windows 若报 `NoDefaultCurrentDirectoryInExePath`，用 `env -u NoDefaultCurrentDirectoryInExePath mvn -q test`）
Expected: 全模块 PASS（重点 gateway-iam、gateway-web、gateway-proxy 无回归）

- [ ] **Step 2: 更新 api-spec.md 授权说明**

在 `docs/api-spec.md` 授权/鉴权相关章节补充：

```markdown
### 统一授权模型（资源-动作-范围，v1.x）

授权判定经统一门面 `AuthorizationService`，以「资源-动作-范围」统一模型承载两面授权：

| 面 | 主体 | 授权判定 | 范围 |
|----|------|---------|------|
| 控制面（管理 API `/api/v1/**`） | 用户（Identity.role） | 代码化权限表（PUBLIC / LOGIN_ONLY / USER 白名单 / 默认拒绝仅 ADMIN） | ALL / 登录即可 / 角色 |
| 数据面（代理 API `/v1/**`） | 应用（applicationId 权限锚点） | 应用-渠道授权（`application_channel`，D9 无角色特权旁路） | 应用可见渠道集合 |

控制面规则表代码化于 iam 域（`AuthorizationService.CONTROL_RULES`），与前端 `RolePermissions` 权限码语义对齐；
数据面渠道授权由 `PermissionRouter` 经统一门面查询，过滤语义不变（可见渠道为空 → 路由空候选）。
```

- [ ] **Step 3: 提交**

```bash
git add docs/api-spec.md
git commit -m "docs: 补充统一授权模型说明（AuthorizationService 资源-动作-范围判定）"
```

---

## Self-Review 记录

**1. Spec 覆盖**（对照评估结论）：
- 统一门面 `AuthorizationService`（控制面判定 + 数据面委托）✔ Task 1
- 控制面规则代码化（资源-动作-范围，不落 DB）✔ Task 1 `CONTROL_RULES`
- `PermissionInterceptor` 退化为门面调用（静态规则移除）✔ Task 2
- 数据面接入统一入口（`PermissionRouter` 改经门面）✔ Task 3
- 零 schema 变更 ✔（无任何表迁移）
- 授权语义零变化（规则逐条对应原四个静态列表）✔ Task 1 规则表与 Task 2 删减对照

**2. 占位符扫描**：无 TBD/TODO；测试与实现代码均完整给出。Task 3 测试注有"实现者读取现测试文件后按上例对齐"的合理说明（原测试 mock 形态以现文件为准）。

**3. 类型一致性**：
- `AuthorizationService.checkControl(Identity, String, String)` → Task 2 消费一致 ✔
- `AuthorizationService.permittedChannelIds(Long)` → Task 3 消费一致 ✔
- `Identity.role()` 与 `RolePermissions.ROLE_USER/ROLE_ADMIN`（`"USER"`/`"ADMIN"`）比对一致 ✔
- `ControlPermissionRule` 五参构造（resource/action/scope/method/pathPattern）在 Task 1 定义与使用一致 ✔

**已知边界（刻意保留）**：
- `RolePermissions` 权限码（前端显隐）不改动；`AuthorizationService` 规则与其语义对齐但不同源（阶段 2 再收敛单一事实源）
- 数据面 scope 表达式（阶段 3 表化）不在本计划范围；`permittedChannelIds` 委托保持现有查询
- 服务层 `StpUtil` owner check 兜底（`UserApiKeyServiceImpl`/`ApplicationServiceImpl`）不迁移（阶段 2 范围）
- 控制面 `SELF` 范围仅体现在规则语义（user-api-keys 单条），实际 owner check 仍由服务层执行（阶段 2 收敛）
