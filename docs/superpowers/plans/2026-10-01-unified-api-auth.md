# 统一 API 认证（入站代理 + 管理面）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将入站 API 代理认证（API Key）与管理 API 认证（Sa-Token 会话）统一为「单一认证拦截器 + 统一 Identity 身份产物」，消除双 attribute 双轨，保留两套凭证机制与安全边界。

**Architecture:** 在 gateway-web 拦截器层新增 `GatewayAuthenticatorInterceptor`（order=1）替代 `ApiKeyAuthInterceptor` + `TokenAuthInterceptor`：按路径分类（代理端点 → API Key 认证；`/api/v1/` → 会话认证），统一产出 `Identity` 注入 `identity` request attribute。iam 域新增 `SessionAuthenticationService` 承载会话→身份构建。`PermissionInterceptor`/`AuditLogInterceptor`/`AuthController`/`MeController` 全部改为消费 `Identity`。授权模型（数据面 applicationId 锚点 / 控制面角色白名单）保持不变。

**Tech Stack:** Java 21 + Spring Boot 3.5.x + Sa-Token + Mockito（mockStatic 静态桩） + JUnit 5 + AssertJ

## Global Constraints

- 分层铁律：gateway-iam 域核心**不得**依赖 jakarta.servlet；路径分类是 Web 承载层关注点，只放 `gateway-web`
- 401 语义不变（`docs/api-spec.md:1209`）：代理端点 `{"code":"UNAUTHORIZED","message":"无效的 API Key"}`；管理面 `"请先登录"`
- 认证方式不变：代理端点接受 `Authorization: Bearer` 或 `x-api-key`；管理面保留 Sa-Token 会话
- 授权模型不变：数据面 `applicationId` 锚点（`PermissionRouter`）、控制面 USER/ADMIN 角色白名单
- 服务层 `StpUtil.hasRole` 兜底（`UserApiKeyServiceImpl`/`ApplicationServiceImpl`）与 `StpRoleService`（StpInterface）**保留不动**——它们是服务层 owner check 依赖
- 拦截器链 order：IPBlock(0) → Authenticator(1) → Permission(3) → RateLimit(4)；AuditLog 为独立 Spring `HandlerInterceptor`（WebConfig 注册）
- 代码注释、Javadoc、commit message 一律中文（项目语言规范）
- 模块坐标（根 pom 聚合）：`gateway-iam/iam`、`gateway-web`；测试命令 `mvn -q -pl <module> -am test`（`-am` 连带构建依赖模块，避免 SNAPSHOT 未安装）
- SSE 异步分发（`DispatcherType.ASYNC`）：认证信息在初始请求已注入 `identity` attribute，async 阶段直接恢复不重复认证

---

### Task 1: SessionAuthenticationService（iam 域会话→身份构建）

**Files:**
- Create: `gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/SessionAuthenticationService.java`
- Test: `gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/SessionAuthenticationServiceTest.java`

**Interfaces:**
- Consumes: `cn.dev33.satoken.stp.StpUtil`（静态）、`com.codingas.gateway.iam.user.UserRepository`、`com.codingas.gateway.iam.user.User`
- Produces: `Optional<Identity> authenticateSession()` —— 已登录返回 `Identity.of(userId, users.role, null, null)`（credentialId/applicationId 为 null，语义与数据面区分）；未登录返回 `Optional.empty()`。Task 2 依赖此签名。

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.iam.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionAuthenticationService（会话认证服务）测试")
class SessionAuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    private SessionAuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new SessionAuthenticationService(userRepository);
    }

    @Test
    @DisplayName("已登录：返回携带真实角色的身份（管理面凭据字段为 null）")
    void loggedIn_returnsIdentityWithRole() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            User user = mock(User.class);
            when(user.getRole()).thenReturn("ADMIN");
            when(userRepository.findById(42L)).thenReturn(Optional.of(user));

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isPresent();
            Identity identity = result.get();
            assertThat(identity.userId()).isEqualTo(42L);
            assertThat(identity.role()).isEqualTo("ADMIN");
            assertThat(identity.credentialId()).isNull();
            assertThat(identity.applicationId()).isNull();
        }
    }

    @Test
    @DisplayName("未登录：返回空")
    void notLoggedIn_returnsEmpty() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(false);

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }
    }

    @Test
    @DisplayName("会话存在但用户已删除：返回角色为 null 的身份（由授权层拒绝）")
    void sessionWithoutUser_returnsIdentityWithNullRole() {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::isLogin).thenReturn(true);
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(42L);
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            Optional<Identity> result = service.authenticateSession();

            assertThat(result).isPresent();
            assertThat(result.get().role()).isNull();
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=SessionAuthenticationServiceTest`
Expected: 编译失败 `cannot find symbol: class SessionAuthenticationService`

- [ ] **Step 3: 最小实现**

```java
package com.codingas.gateway.iam.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 会话认证服务（管理面）
 *
 * <p>基于 Sa-Token 会话状态构建统一身份 {@link Identity}：
 * 已登录 → 查 users.role 填充角色（角色级授权的事实源）；未登录 → 返回空。
 * 管理面身份的 credentialId / applicationId 恒为 null（数据面权限锚点仅属 API Key 身份）。</p>
 */
@Service
public class SessionAuthenticationService {

    private final UserRepository userRepository;

    public SessionAuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 认证当前会话并构建统一身份
     *
     * @return 已登录时的身份；未登录返回空
     */
    public Optional<Identity> authenticateSession() {
        if (!StpUtil.isLogin()) {
            return Optional.empty();
        }
        Long userId = StpUtil.getLoginIdAsLong();
        String role = userRepository.findById(userId)
                .map(User::getRole)
                .orElse(null);
        return Optional.of(Identity.of(userId, role, null, null));
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-iam/iam -am test -Dtest=SessionAuthenticationServiceTest`
Expected: PASS（3 个测试全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-iam/iam/src/main/java/com/codingas/gateway/iam/auth/SessionAuthenticationService.java gateway-iam/iam/src/test/java/com/codingas/gateway/iam/auth/SessionAuthenticationServiceTest.java
git commit -m "feat(iam): 新增 SessionAuthenticationService 会话→身份构建服务"
```

---

### Task 2: GatewayAuthenticatorInterceptor（统一认证拦截器，替代双拦截器）

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptor.java`
- Delete: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/ApiKeyAuthInterceptor.java`
- Delete: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/TokenAuthInterceptor.java`
- Delete: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/ApiKeyAuthInterceptorTest.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/GatewayAuthenticatorInterceptorTest.java`

**Interfaces:**
- Consumes: Task 1 的 `SessionAuthenticationService.authenticateSession()`（`Optional<Identity>`）；现有 `AuthenticationService.authenticateUser(String)`（`Identity`，抛 `AuthenticationFailedException`）；`Identity`
- Produces: `GatewayAuthenticatorInterceptor`（name=`Authenticator`，order=1）——代理端点与管理端点统一注入 `identity` request attribute。Task 3/4 依赖「`identity` attribute 恒为 `Identity` 类型」这一约定。

- [ ] **Step 1: 写失败测试**

```java
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.auth.AuthenticationService;
import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.iam.auth.SessionAuthenticationService;
import jakarta.servlet.DispatcherType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayAuthenticatorInterceptor（统一认证拦截器）测试")
class GatewayAuthenticatorInterceptorTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private SessionAuthenticationService sessionAuthenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private GatewayAuthenticatorInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new GatewayAuthenticatorInterceptor(authenticationService, sessionAuthenticationService);
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        lenient().when(request.getDispatcherType()).thenReturn(DispatcherType.REQUEST);
    }

    // ---------- 代理端点：API Key 认证 ----------

    @Test
    @DisplayName("代理端点有效 API Key（Bearer）通过并注入 identity")
    void proxyPath_validBearer_passes() {
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("Authorization")).thenReturn("Bearer sk-test123");
        Identity identity = Identity.of(1L, "user", 1L, 10L);
        when(authenticationService.authenticateUser("sk-test123")).thenReturn(identity);

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verify(request).setAttribute("identity", identity);
    }

    @Test
    @DisplayName("代理端点有效 API Key（x-api-key）通过")
    void proxyPath_validXApiKey_passes() {
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("x-api-key")).thenReturn("sk-test123");
        Identity identity = Identity.of(1L, "user", 1L, 10L);
        when(authenticationService.authenticateUser("sk-test123")).thenReturn(identity);

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verify(request).setAttribute("identity", identity);
    }

    @Test
    @DisplayName("代理端点缺少 API Key → 401 无效的 API Key")
    void proxyPath_missingKey_rejected401() throws Exception {
        when(request.getRequestURI()).thenReturn("/v1/chat/completions");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("x-api-key")).thenReturn(null);

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
        verify(response.getWriter()).write(contains("无效的 API Key"));
    }

    @Test
    @DisplayName("代理端点认证失败 → 401 无效的 API Key")
    void proxyPath_authFailed_rejected401() throws Exception {
        when(request.getRequestURI()).thenReturn("/v1/messages");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-key");
        when(authenticationService.authenticateUser("bad-key"))
                .thenThrow(new AuthenticationFailedException("无效的 API Key"));

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("Anthropic 代理端点 /anthropic/v1/messages 走 API Key 认证")
    void proxyPath_anthropicMessages_passes() {
        when(request.getRequestURI()).thenReturn("/anthropic/v1/messages");
        when(request.getHeader("Authorization")).thenReturn("Bearer sk-test123");
        Identity identity = Identity.of(1L, "user", 1L, 10L);
        when(authenticationService.authenticateUser("sk-test123")).thenReturn(identity);

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verify(request).setAttribute("identity", identity);
        verifyNoInteractions(sessionAuthenticationService);
    }

    // ---------- 管理端点：会话认证 ----------

    @Test
    @DisplayName("管理端点已登录 → 注入 identity 通过")
    void managedPath_loggedIn_passes() {
        when(request.getRequestURI()).thenReturn("/api/v1/channels");
        Identity identity = Identity.of(42L, "ADMIN", null, null);
        when(sessionAuthenticationService.authenticateSession()).thenReturn(Optional.of(identity));

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verify(request).setAttribute("identity", identity);
        verifyNoInteractions(authenticationService);
    }

    @Test
    @DisplayName("管理端点未登录 → 401 请先登录")
    void managedPath_notLoggedIn_rejected401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/channels");
        when(sessionAuthenticationService.authenticateSession()).thenReturn(Optional.empty());

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
        verify(response.getWriter()).write(contains("请先登录"));
    }

    @Test
    @DisplayName("公开路径（auth/login、auth/logout）放行不认证")
    void managedPath_publicPaths_pass() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        assertThat(interceptor.preHandle(request, response)).isTrue();
        when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");
        assertThat(interceptor.preHandle(request, response)).isTrue();
        verifyNoInteractions(sessionAuthenticationService);
        verifyNoInteractions(authenticationService);
    }

    // ---------- 其他路径 ----------

    @Test
    @DisplayName("非代理非管理路径放行（静态资源等）")
    void otherPath_passes() {
        when(request.getRequestURI()).thenReturn("/assets/app.js");
        assertThat(interceptor.preHandle(request, response)).isTrue();
        verifyNoInteractions(sessionAuthenticationService);
        verifyNoInteractions(authenticationService);
    }

    // ---------- SSE 异步分发 ----------

    @Test
    @DisplayName("SSE 异步分发：从 attribute 恢复身份，不重复认证")
    void asyncDispatch_restoresIdentity() {
        Identity identity = Identity.of(1L, "user", 1L, 10L);
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        when(request.getAttribute("identity")).thenReturn(identity);

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verifyNoInteractions(sessionAuthenticationService);
        verifyNoInteractions(authenticationService);
        verify(request, never()).setAttribute(eq("identity"), any());
    }

    @Test
    @DisplayName("SSE 异步分发且身份缺失：放行并告警（初始请求已校验）")
    void asyncDispatch_missingIdentity_passes() {
        when(request.getDispatcherType()).thenReturn(DispatcherType.ASYNC);
        when(request.getAttribute("identity")).thenReturn(null);

        assertThat(interceptor.preHandle(request, response)).isTrue();
        verifyNoInteractions(sessionAuthenticationService);
        verifyNoInteractions(authenticationService);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-web -am test -Dtest=GatewayAuthenticatorInterceptorTest`
Expected: 编译失败 `cannot find symbol: class GatewayAuthenticatorInterceptor`

- [ ] **Step 3: 最小实现**

```java
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.auth.AuthenticationService;
import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.iam.auth.SessionAuthenticationService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 统一认证拦截器
 *
 * <p>替代 {@code ApiKeyAuthInterceptor} 与 {@code TokenAuthInterceptor} 两个拦截器，
 * 按路径分类统一完成认证，并统一产出 {@link Identity} 注入 {@code identity} request attribute：</p>
 * <ul>
 *   <li><b>代理端点</b>（{@code /v1/chat/completions}、{@code /v1/messages}、{@code /v1/models}、
 *       {@code /anthropic/v1/messages}、{@code /anthropic/v1/models}）：API Key 认证
 *       （{@code Authorization: Bearer} 或 {@code x-api-key}）；</li>
 *   <li><b>管理端点</b>（{@code /api/v1/}，除公开路径）：Sa-Token 会话认证；</li>
 *   <li>其余路径放行。</li>
 * </ul>
 *
 * <p>401 语义与 {@code docs/api-spec.md} 一致：代理端点 {@code 无效的 API Key}，管理面 {@code 请先登录}。</p>
 * <p>SSE 异步分发阶段不重复认证：身份已在初始请求注入，直接恢复。</p>
 */
@Slf4j
@Component
public class GatewayAuthenticatorInterceptor extends AbstractGatewayInterceptor {

    /** 代理端点路径前缀（API Key 认证） */
    private static final List<String> PROXY_PATHS = List.of(
            "/v1/chat/completions",
            "/v1/messages",
            "/v1/models",
            "/anthropic/v1/messages",
            "/anthropic/v1/models"
    );

    /** 管理 API 路径前缀（会话认证） */
    private static final String MANAGED_PREFIX = "/api/v1/";

    /** 公开路径（无需认证） */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/logout"
    );

    private final AuthenticationService authenticationService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public GatewayAuthenticatorInterceptor(AuthenticationService authenticationService,
                                           SessionAuthenticationService sessionAuthenticationService) {
        this.authenticationService = authenticationService;
        this.sessionAuthenticationService = sessionAuthenticationService;
    }

    @Override
    public String name() {
        return "Authenticator";
    }

    @Override
    public int order() {
        return 1; // IPBlock(0) 之后、Permission(3) 之前
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // SSE 异步分发：身份已在初始请求注入，恢复即可，不重复认证
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            if (request.getAttribute("identity") == null) {
                log.warn("SSE 异步分发未携带 identity，初始请求可能未认证: path={}", request.getRequestURI());
            }
            return true;
        }

        String path = request.getRequestURI();

        if (isProxyPath(path)) {
            return authenticateApiKey(request, response, path);
        }

        if (path.startsWith(MANAGED_PREFIX)) {
            return PUBLIC_PATHS.contains(path)
                    || authenticateSession(request, response, path);
        }

        return true;
    }

    /** 代理端点：API Key 认证 */
    private boolean authenticateApiKey(HttpServletRequest request, HttpServletResponse response, String path) {
        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("请求缺少 API Key: path={}", path);
            return rejectUnauthorized(response, "无效的 API Key");
        }
        try {
            Identity identity = authenticationService.authenticateUser(apiKey);
            request.setAttribute("identity", identity);
            return true;
        } catch (AuthenticationFailedException e) {
            log.warn("认证失败: path={}, reason={}", path, e.getMessage());
            return rejectUnauthorized(response, "无效的 API Key");
        }
    }

    /** 管理端点：会话认证 */
    private boolean authenticateSession(HttpServletRequest request, HttpServletResponse response, String path) {
        Optional<Identity> identityOpt = sessionAuthenticationService.authenticateSession();
        if (identityOpt.isEmpty()) {
            log.warn("会话认证失败: path={}", path);
            return rejectUnauthorized(response, "请先登录");
        }
        request.setAttribute("identity", identityOpt.get());
        return true;
    }

    /** 从 Authorization 或 x-api-key header 提取 API Key */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getHeader("x-api-key");
    }

    /** 判断是否为代理端点路径 */
    private boolean isProxyPath(String path) {
        for (String proxyPath : PROXY_PATHS) {
            if (path.startsWith(proxyPath)) {
                return true;
            }
        }
        return false;
    }

    /** 短路 401 */
    private boolean rejectUnauthorized(HttpServletResponse response, String message) {
        try {
            unauthorized(response, message);
        } catch (Exception e) {
            log.error("Failed to write unauthorized response", e);
        }
        return false;
    }
}
```

- [ ] **Step 4: 删除旧拦截器及其测试**

```bash
git rm gateway-web/src/main/java/com/codingas/gateway/web/interceptor/ApiKeyAuthInterceptor.java
git rm gateway-web/src/main/java/com/codingas/gateway/web/interceptor/TokenAuthInterceptor.java
git rm gateway-web/src/test/java/com/codingas/gateway/web/interceptor/ApiKeyAuthInterceptorTest.java
```

注意：`TokenAuthInterceptor` 的 SSE 异步分支已并入新拦截器（Step 3 `DispatcherType.ASYNC` 分支）；`ApiKeyAuthInterceptor` 的 `isProxyPath` 语义原样保留（`PROXY_PATHS` 前缀匹配，与旧实现 `startsWith` 一致）。行为差异为零，除删除 `userId` attribute（下游统一读 `identity`，Task 3/4 同步改造）。

- [ ] **Step 5: 运行全模块相关测试确认通过**

Run: `mvn -q -pl gateway-web -am test`
Expected: PASS（新拦截器测试 10 个场景全绿；`SecurityInterceptorChainTest` 使用 mock 拦截器不受成员变化影响；`WebConfig` 无改动——链按 order 自动排序）

- [ ] **Step 6: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/interceptor/ gateway-web/src/test/java/com/codingas/gateway/web/interceptor/
git commit -m "feat(web): 统一认证拦截器替代 ApiKeyAuth/TokenAuth 双拦截器，统一注入 Identity"
```

---

### Task 3: PermissionInterceptor 从统一身份读取角色

**Files:**
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/PermissionInterceptor.java`
- Modify: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/PermissionInterceptorTest.java`

**Interfaces:**
- Consumes: `identity` request attribute（`Identity` 类型，Task 2 注入）；`RolePermissions.ROLE_ADMIN`/`ROLE_USER`（值 `"ADMIN"`/`"USER"`）
- Produces: 无新接口。行为不变：PUBLIC/LOGIN_ONLY 规则放行；ADMIN 全通；USER 白名单；其他 403。

- [ ] **Step 1: 改写失败测试（替换 mockStatic StpUtil 为 identity attribute 桩）**

关键改造点：`stubRole(String role)` 改为在 request 上 `setAttribute("identity", Identity.of(1L, role, null, null))`；未登录场景（`identity == null`）单独保留。

```java
package com.codingas.gateway.web.interceptor;

import com.codingas.gateway.iam.auth.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * {@link PermissionInterceptor} 角色级授权测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionInterceptor（角色级授权）测试")
class PermissionInterceptorTest {

    private final PermissionInterceptor interceptor = new PermissionInterceptor();

    private HttpServletRequest request(String method, String uri, String role) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        lenient().when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        if (role != null) {
            when(req.getAttribute("identity")).thenReturn(Identity.of(1L, role, null, null));
        }
        return req;
    }

    private HttpServletResponse response() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        lenient().when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return resp;
    }

    @Test
    @DisplayName("公开路径（登录接口）放行，不校验身份")
    void publicPath_passes() throws Exception {
        // 未 mock identity attribute 仍放行，即证明公开路径不经过角色分支
        assertThat(interceptor.preHandle(request("POST", "/api/v1/auth/login", null), response())).isTrue();
    }

    @Test
    @DisplayName("登录即可路径（auth/me）放行")
    void loginOnlyPath_passes() throws Exception {
        assertThat(interceptor.preHandle(request("GET", "/api/v1/auth/me", null), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/me/api-keys", null), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/protocols", null), response())).isTrue();
    }

    @Test
    @DisplayName("ADMIN 访问管理端点放行")
    void admin_manageEndpoints_pass() throws Exception {
        assertThat(interceptor.preHandle(request("DELETE", "/api/v1/users/1", "ADMIN"), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/channels", "ADMIN"), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/stats", "ADMIN"), response())).isTrue();
    }

    @Test
    @DisplayName("USER 访问白名单（模型读/体验/自己的 Key）放行")
    void user_allowedPaths_pass() throws Exception {
        assertThat(interceptor.preHandle(request("GET", "/api/v1/models", "USER"), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/applications", "USER"), response())).isTrue();
        assertThat(interceptor.preHandle(request("POST", "/api/v1/experience/chat", "USER"), response())).isTrue();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/user-api-keys/1/detail", "USER"), response())).isTrue();
    }

    @Test
    @DisplayName("USER 访问管理端点 → 403 拒绝")
    void user_manageEndpoints_rejected403() throws Exception {
        HttpServletResponse resp = response();
        assertThat(interceptor.preHandle(request("DELETE", "/api/v1/users/1", "USER"), resp)).isFalse();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/channels", "USER"), response())).isFalse();
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("身份缺失（未登录但绕过认证）→ 403 拒绝")
    void missingIdentity_rejected403() throws Exception {
        HttpServletResponse resp = response();
        assertThat(interceptor.preHandle(request("GET", "/api/v1/models", null), resp)).isFalse();
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("未知角色访问管理端点 → 403 拒绝")
    void unknownRole_manageEndpoints_rejected403() throws Exception {
        assertThat(interceptor.preHandle(request("GET", "/api/v1/models", "OTHER"), response())).isFalse();
    }

    @Test
    @DisplayName("API Key 网关路径（/v1/）跳过授权")
    void apiKeyPath_skips() throws Exception {
        assertThat(interceptor.preHandle(request("POST", "/v1/chat/completions", null), response())).isTrue();
    }

    @Test
    @DisplayName("非管理路径（静态资源等）跳过")
    void nonManagedPath_skips() throws Exception {
        assertThat(interceptor.preHandle(request("GET", "/assets/app.js", null), response())).isTrue();
    }
}
```

注意：原测试中 `request(String, String)` 签名改为三参（新增 role）；`publicPath_passes` 中原 `stp.verifyNoInteractions()` 的「不校验角色」语义等价于 `getAttribute` 不被调用——但 Spring 在管理路径会先走 PUBLIC/LOGIN_ONLY 匹配，未到角色分支。为简化，该断言改为放行断言即可（`verify(...never()).getAttribute` 因每次调用新建 mock 无法验证，删除该行，语义由「不 mock identity 仍放行」保证）。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -q -pl gateway-web -am test -Dtest=PermissionInterceptorTest`
Expected: FAIL（实现仍读 `StpUtil.hasRole`，mockStatic 移除后 `hasRole` 恒 false → USER 放行场景失败）

- [ ] **Step 3: 修改实现**

`PermissionInterceptor.preHandle` 中角色判断段（原 127-133 行附近）替换为：

```java
        // 角色授权：身份由 GatewayAuthenticatorInterceptor 注入（identity attribute）
        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null) {
            log.warn("认证身份缺失: {} {} 被拒绝", method, uri);
            return rejectForbidden(response, "无访问权限");
        }
        String role = identity.role();
        if (RolePermissions.ROLE_ADMIN.equals(role)) {
            return true;
        }
        if (RolePermissions.ROLE_USER.equals(role) && matches(USER_ALLOWED_RULES, method, uri)) {
            return true;
        }

        log.warn("角色权限不足: {} {} 被拒绝", method, uri);
        return rejectForbidden(response, "无访问权限");
```

同时更新类 Javadoc 与 import：
- 删除 `import cn.dev33.satoken.stp.StpUtil;`
- 新增 `import com.codingas.gateway.iam.auth.Identity;`
- Javadoc 中「基于 USER/ADMIN 两角色授权」补充：角色取自统一身份 `Identity.role()`（users.role 唯一事实源，由 `SessionAuthenticationService` 填充）

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -q -pl gateway-web -am test -Dtest=PermissionInterceptorTest`
Expected: PASS（9 个测试全绿）

- [ ] **Step 5: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/interceptor/PermissionInterceptor.java gateway-web/src/test/java/com/codingas/gateway/web/interceptor/PermissionInterceptorTest.java
git commit -m "refactor(web): PermissionInterceptor 改读统一身份 Identity.role，移除 StpUtil 依赖"
```

---

### Task 4: 消费方统一（AuditLogInterceptor + AuthController + MeController）

**Files:**
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/interceptor/AuditLogInterceptor.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/AuthController.java`
- Modify: `gateway-web/src/main/java/com/codingas/gateway/web/api/MeController.java`
- Modify: `gateway-web/src/test/java/com/codingas/gateway/web/interceptor/AuditLogInterceptorTest.java`

**Interfaces:**
- Consumes: `identity` request attribute（`Identity`，Task 2 注入）。注意 `AuditLogInterceptor` 在安全链之外独立注册（WebConfig），同一请求先于安全链执行 `preHandle`、后于安全链 `afterCompletion`——身份注入发生在安全链 preHandle，`afterCompletion` 读取时已存在
- Produces: 无新接口。审计 `userId` 语义不变（未认证主体记 0）

- [ ] **Step 1: 更新 AuditLogInterceptor 测试（identity → userId 读取路径变更）**

现有 `AuditLogInterceptorTest` 中 `userId` attribute 桩改为 `identity` attribute 桩：

```java
    // 原：when(request.getAttribute("userId")).thenReturn(42L);
    // 改：
    when(request.getAttribute("identity")).thenReturn(Identity.of(42L, "ADMIN", null, null));
```

并新增「identity 缺失记 0」用例：

```java
    @Test
    @DisplayName("未认证主体（identity 缺失）以 0 记录操作人")
    void missingIdentity_recordsZeroUserId() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/channels");
        when(request.getMethod()).thenReturn("POST");
        when(request.getAttribute("identity")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        interceptor.afterCompletion(request, response, null, null);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().userId()).isZero();
    }
```

- [ ] **Step 2: 修改 AuditLogInterceptor**

`afterCompletion` 中身份读取段（原 72-74 行附近）替换：

```java
            // 操作人取统一身份 Identity（GatewayAuthenticatorInterceptor 注入）；
            // 未认证主体（如登录请求）以 0 记录，规避 audit_logs.user_id NOT NULL 约束
            Identity identity = (Identity) request.getAttribute("identity");
            AuditEvent event = AuditEvent.builder()
                    .userId(identity != null && identity.userId() != null ? identity.userId() : 0L)
                    .action(method + " " + uri)
                    .resource(uri)
                    .clientIp(getClientIp(request))
                    .responseStatus(response.getStatus())
                    .occurredOn(Instant.now())
                    .build();
```

新增 import `com.codingas.gateway.iam.auth.Identity`；类 Javadoc「操作人取 {@link TokenAuthInterceptor} 注入的 {@code userId}」改为「操作人取统一认证拦截器注入的 {@code identity}」。

- [ ] **Step 3: 修改 AuthController（getCurrentUser / updatePassword 改读 identity）**

```java
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser(@RequestAttribute("identity") Identity identity) {
        return UserResponse.from(userService.getById(identity.userId()));
    }

    /**
     * 修改密码
     */
    @PatchMapping("/me/password")
    public void updatePassword(@RequestAttribute("identity") Identity identity,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(identity.userId(), request.currentPassword(), request.newPassword());
    }
```

- 新增 import：`com.codingas.gateway.iam.auth.Identity`（`RequestAttribute` 已由现有 `org.springframework.web.bind.annotation.*` 通配符导入覆盖，无需新增）
- 保留 `import cn.dev33.satoken.stp.StpUtil;`（`logout()` 方法仍用 `StpUtil.logout()`）

- [ ] **Step 4: 修改 MeController（listMyApiKeys 改读 identity）**

```java
    /**
     * 查询当前用户的所有 API Key
     */
    @GetMapping("/api-keys")
    public List<UserApiKeyResponse> listMyApiKeys(@RequestAttribute("identity") Identity identity) {
        return UserApiKeyResponse.from(userApiKeyService.findByUserId(identity.userId()));
    }
```

- 新增 import：`com.codingas.gateway.iam.auth.Identity`、`org.springframework.web.bind.annotation.RequestAttribute`（MeController 使用显式 import，非通配符）
- 删除 `import cn.dev33.satoken.stp.StpUtil;`

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn -q -pl gateway-web -am test`
Expected: PASS（AuditLogInterceptorTest 更新后全绿；AuthController/MeController 无现有单测，编译通过即可；`UserApiKeyServiceImpl`/`ApplicationServiceImpl` 服务层 `StpUtil` 兜底保留，不受影响）

- [ ] **Step 6: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/interceptor/AuditLogInterceptor.java gateway-web/src/main/java/com/codingas/gateway/web/api/AuthController.java gateway-web/src/main/java/com/codingas/gateway/web/api/MeController.java gateway-web/src/test/java/com/codingas/gateway/web/interceptor/AuditLogInterceptorTest.java
git commit -m "refactor(web): 管理面消费方统一改读 identity attribute（审计/Auth/Me）"
```

---

### Task 5: 全量回归与文档对齐

**Files:**
- Modify: `docs/api-spec.md`（认证章节补统一认证说明）

- [ ] **Step 1: 全量回归测试**

Run: `mvn -q test`（根目录全量；Windows 下若遇 `NoDefaultCurrentDirectoryInExePath` 报错，改用 `env -u NoDefaultCurrentDirectoryInExePath mvn -q test`）
Expected: 全模块 PASS（重点确认 gateway-iam、gateway-web、gateway-boot 无回归）

- [ ] **Step 2: 更新 api-spec.md 认证说明**

在 `docs/api-spec.md` 认证相关章节（约 7.13 认证 / 认证方式表附近）补充一段：

```markdown
### 统一认证架构（v1.x）

入站认证由统一认证拦截器 `GatewayAuthenticatorInterceptor` 完成，按路径分类认证并统一产出身份上下文 `Identity`：

| 面 | 路径 | 认证机制 | 身份字段 |
|----|------|---------|---------|
| 数据面（代理） | `/v1/chat/completions`、`/v1/messages`、`/v1/models`、`/anthropic/v1/messages`、`/anthropic/v1/models` | API Key（`Authorization: Bearer` 或 `x-api-key`） | `userId`/`role`/`credentialId`/`applicationId` |
| 控制面（管理） | `/api/v1/**`（除 `auth/login`、`auth/logout` 公开路径） | Sa-Token 会话 | `userId`/`role`（`credentialId`/`applicationId` 为 null） |

两套凭证机制保留（API Key 无状态 / 会话有状态），授权边界不变：数据面按 `applicationId` 路由可见渠道，控制面按 USER/ADMIN 角色校验白名单。
```

- [ ] **Step 3: 提交**

```bash
git add docs/api-spec.md
git commit -m "docs: 补充统一认证架构说明（GatewayAuthenticatorInterceptor 路径分类与身份模型）"
```

---

## Self-Review 记录

**1. Spec 覆盖**（对照评估报告阶段 0-2）：
- 阶段 0（注释卫生）：Task 2 删除含过期 order 注释的旧拦截器，问题自然消除 ✔
- 阶段 1（统一身份模型）：Task 1 + Task 2 + Task 4（身份产物统一为 `identity` attribute）✔
- 阶段 2（认证门面）：Task 2 统一拦截器（路径分类门面）+ iam 域两类凭证认证服务 ✔
- 保留项（明确不动）：`StpRoleService`、服务层 `StpUtil` 兜底、`AuthenticationService.authenticateUser` 数据面角色语义（硬编码 "user" 为既有简化，不在本计划范围）✔

**2. 占位符扫描**：无 TBD/TODO；所有测试与实现代码均已给出完整内容。

**3. 类型一致性**：
- `SessionAuthenticationService.authenticateSession()` 返回 `Optional<Identity>`，Task 2 消费一致 ✔
- `Identity.of(Long, String, Long, Long)` 四处调用参数类型一致 ✔
- Task 3 `identity.role()` 与 `RolePermissions.ROLE_ADMIN/ROLE_USER`（`"ADMIN"`/`"USER"`）比对一致 ✔
- `request.setAttribute("identity", identity)` 注入与 `@RequestAttribute("identity") Identity` 消费一致 ✔

**已知边界（行为保持不变，刻意不处理）**：
- `/v1/` 下非代理路径（如未来新增的非代理端点）无认证——与现状一致，未扩大认证范围
- 数据面 `Identity.role` 硬编码 `"user"`（`AuthenticationService` 既有简化），本计划不改变数据面角色语义
