# Security Interceptor Integration Implementation Plan (Chain of Responsibility)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用责任链模式集成 IP 封锁检查到认证拦截器，并为敏感数据脱敏添加全局响应拦截器

**Architecture:**
- Task 1: 创建 `SecurityInterceptorChain` 责任链管理器，支持多拦截器组合
- Task 2: 创建 `IPBlockCheckInterceptor` IP 封锁拦截器
- Task 3: 重构 `ApiKeyAuthInterceptor` 成为独立拦截器
- Task 4: 创建 `MaskingResponseAdvice` 全局响应脱敏器
- Task 5: 集成测试和覆盖率验证

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring MVC, JUnit 5, AssertJ, Mockito

---

## File Structure

```
gateway-application/src/main/java/com/codingas/gateway/config/satoken/
├── SaTokenConfig.java                              # 修改：使用 SecurityInterceptorChain
├── ApiKeyAuthInterceptor.java                      # 新建：继承 GatewayInterceptor
├── IPBlockCheckInterceptor.java                   # 新建：IP 封锁拦截器

gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/
├── GatewayInterceptor.java                         # 新建：拦截器基础接口
├── SecurityInterceptorChain.java                   # 新建：责任链管理器
└── AbstractGatewayInterceptor.java                  # 新建：拦截器抽象基类

gateway-web/src/main/java/com/codingas/gateway/web/advice/
├── MaskingResponseAdvice.java                      # 新建：全局响应脱敏
└── GlobalExceptionHandler.java                     # 已存在

gateway-web/src/test/java/com/codingas/gateway/web/advice/
├── IPBlockCheckInterceptorTest.java                 # 新建
├── ApiKeyAuthInterceptorTest.java                   # 新建
├── MaskingResponseAdviceTest.java                  # 新建
└── MaskingTestController.java                       # 新建：测试控制器
```

---

## Precondition

```bash
# 确认相关文件存在
ls gateway-core/src/main/java/com/codingas/gateway/core/security/ipblock/IpBlocklistService.java
ls gateway-core/src/main/java/com/codingas/gateway/core/security/masking/SensitiveDataMasker.java
ls gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java
```

---

## Task 1: 创建 GatewayInterceptor 基础接口和 SecurityInterceptorChain

**Files:**
- Create: `gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/GatewayInterceptor.java`
- Create: `gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/AbstractGatewayInterceptor.java`
- Create: `gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChain.java`
- Test: `gateway-core/src/test/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChainTest.java`

- [ ] **Step 1: 创建 GatewayInterceptor 接口**

```java
// gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/GatewayInterceptor.java
package com.codingas.gateway.core.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 网关拦截器接口
 *
 * <p>所有安全拦截器实现此接口，支持责任链模式。</p>
 */
public interface GatewayInterceptor {

    /**
     * 拦截器名称
     */
    String name();

    /**
     * 处理请求
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @return true 继续执行后续拦截器，false 短路处理
     */
    boolean preHandle(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取执行顺序（数字越小越靠前）
     */
    int order();
}
```

- [ ] **Step 2: 创建 AbstractGatewayInterceptor 抽象基类**

```java
// gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/AbstractGatewayInterceptor.java
package com.codingas.gateway.core.security.interceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * 网关拦截器抽象基类
 *
 * <p>提供通用的日志记录和便捷方法。</p>
 */
@Slf4j
public abstract class AbstractGatewayInterceptor implements GatewayInterceptor {

    @Override
    public int order() {
        return 0; // 默认顺序，子类可覆盖
    }

    /**
     * 获取客户端真实 IP（支持代理场景）
     */
    protected String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 短路处理，返回 403
     */
    protected void reject(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":{\"code\":\"ACCESS_DENIED\",\"message\":\"" + message + "\"}}");
    }

    /**
     * 短路处理，返回 401
     */
    protected void unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}}");
    }
}
```

- [ ] **Step 3: 创建 SecurityInterceptorChain 责任链管理器**

```java
// gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChain.java
package com.codingas.gateway.core.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 安全拦截器责任链管理器
 *
 * <p>管理和执行所有 GatewayInterceptor 实现，按 order 排序执行。</p>
 */
@Slf4j
@Component
public class SecurityInterceptorChain {

    private final List<GatewayInterceptor> interceptors;

    public SecurityInterceptorChain(List<GatewayInterceptor> interceptors) {
        // 按 order 排序，数字小的先执行
        this.interceptors = interceptors.stream()
                .sorted(Comparator.comparingInt(GatewayInterceptor::order))
                .toList();
        log.info("SecurityInterceptorChain initialized with {} interceptors: {}",
                this.interceptors.size(),
                this.interceptors.stream().map(GatewayInterceptor::name).toList());
    }

    /**
     * 执行责任链
     *
     * @return true 所有拦截器都通过，false 任一拦截器拒绝
     */
    public boolean execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        for (GatewayInterceptor interceptor : interceptors) {
            log.debug("Executing interceptor: {}", interceptor.name());
            if (!interceptor.preHandle(request, response)) {
                log.debug("Interceptor {} rejected request", interceptor.name());
                return false;
            }
        }
        return true;
    }

    /**
     * 获取当前拦截器列表（用于测试）
     */
    public List<GatewayInterceptor> getInterceptors() {
        return interceptors;
    }
}
```

- [ ] **Step 4: 创建 SecurityInterceptorChain 单元测试**

```java
// gateway-core/src/test/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChainTest.java
package com.codingas.gateway.core.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityInterceptorChain Tests")
class SecurityInterceptorChainTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Nested
    @DisplayName("execute")
    class ExecuteTests {

        @Test
        @DisplayName("所有拦截器通过时返回 true")
        void allPass_returnsTrue() throws Exception {
            // given
            GatewayInterceptor first = createPassInterceptor("First", 1);
            GatewayInterceptor second = createPassInterceptor("Second", 2);
            var chain = new SecurityInterceptorChain(List.of(first, second));

            // when
            boolean result = chain.execute(request, response);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("第一个拦截器拒绝时返回 false")
        void firstRejects_returnsFalse() throws Exception {
            // given
            GatewayInterceptor rejecter = createRejectInterceptor("Rejecter", 1);
            GatewayInterceptor passer = createPassInterceptor("Passer", 2);
            var chain = new SecurityInterceptorChain(List.of(rejecter, passer));

            // when
            boolean result = chain.execute(request, response);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("按 order 排序执行")
        void sortedByOrder() throws Exception {
            // given
            GatewayInterceptor last = createPassInterceptor("Last", 3);
            GatewayInterceptor first = createPassInterceptor("First", 1);
            GatewayInterceptor middle = createPassInterceptor("Middle", 2);
            var chain = new SecurityInterceptorChain(List.of(last, first, middle));

            // when
            chain.execute(request, response);

            // then - 验证执行顺序（通过 verify 顺序验证）
            inOrder(first, middle, last);
        }
    }

    private GatewayInterceptor createPassInterceptor(String name, int order) {
        return new GatewayInterceptor() {
            @Override
            public String name() { return name; }
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp) { return true; }
            @Override
            public int order() { return order; }
        };
    }

    private GatewayInterceptor createRejectInterceptor(String name, int order) {
        return new GatewayInterceptor() {
            @Override
            public String name() { return name; }
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse resp) { return false; }
            @Override
            public int order() { return order; }
        };
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest=SecurityInterceptorChainTest -pl gateway-core`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/GatewayInterceptor.java
git add gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/AbstractGatewayInterceptor.java
git add gateway-core/src/main/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChain.java
git add gateway-core/src/test/java/com/codingas/gateway/core/security/interceptor/SecurityInterceptorChainTest.java
git commit -m "feat(security): 添加GatewayInterceptor责任链基础架构

- 新增GatewayInterceptor接口定义拦截器契约
- 新增AbstractGatewayInterceptor提供通用方法
- 新增SecurityInterceptorChain责任链管理器
- 支持按order排序执行拦截器

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 创建 IPBlockCheckInterceptor IP 封锁拦截器

**Files:**
- Create: `gateway-application/src/main/java/com/codingas/gateway/config/satoken/IPBlockCheckInterceptor.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/IPBlockCheckInterceptorTest.java`

- [ ] **Step 1: 创建 IPBlockCheckInterceptor 单元测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/IPBlockCheckInterceptorTest.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.config.satoken.IPBlockCheckInterceptor;
import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IPBlockCheckInterceptor Tests")
class IPBlockCheckInterceptorTest {

    @Mock
    private IpBlocklistService ipBlocklistService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private IPBlockCheckInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new IPBlockCheckInterceptor(ipBlocklistService);
    }

    @Test
    @DisplayName("封锁IP返回false")
    void blockedIp_returnsFalse() throws Exception {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");
        when(ipBlocklistService.isBlocked("192.168.1.100")).thenReturn(true);
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("正常IP通过检查")
    void normalIp_returnsTrue() throws Exception {
        // given
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(ipBlocklistService.isBlocked("10.0.0.1")).thenReturn(false);

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("order返回1，最先执行")
    void order_returns1() {
        assertThat(interceptor.order()).isEqualTo(1);
    }

    private static class MockPrintWriter extends java.io.PrintWriter {
        public MockPrintWriter() { super(java.io.OutputStream.nullOutputStream()); }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=IPBlockCheckInterceptorTest -pl gateway-web`
Expected: FAIL - class not found

- [ ] **Step 3: 创建 IPBlockCheckInterceptor**

```java
// gateway-application/src/main/java/com/codingas/gateway/config/satoken/IPBlockCheckInterceptor.java
package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.interceptor.AbstractGatewayInterceptor;
import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * IP 封锁检查拦截器
 *
 * <p>责任链第一个拦截器，在认证前检查 IP 是否被封锁。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class IPBlockCheckInterceptor extends AbstractGatewayInterceptor {

    private final IpBlocklistService ipBlocklistService;

    @Override
    public String name() {
        return "IPBlockCheck";
    }

    @Override
    public int order() {
        return 1; // 最先执行
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String clientIp = getClientIp(request);
        log.debug("Checking IP block for: {}", clientIp);

        if (ipBlocklistService.isBlocked(clientIp)) {
            log.warn("Blocked IP access: ip={}, uri={}", clientIp, request.getRequestURI());
            try {
                reject(response, "Access denied: IP blocked");
            } catch (Exception e) {
                log.error("Failed to write rejection response", e);
            }
            return false;
        }

        return true;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=IPBlockCheckInterceptorTest -pl gateway-web`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add gateway-application/src/main/java/com/codingas/gateway/config/satoken/IPBlockCheckInterceptor.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/IPBlockCheckInterceptorTest.java
git commit -m "feat(security): 添加IPBlockCheckInterceptor拦截器

- 实现GatewayInterceptor接口
- 在认证前检查IP封锁
- 支持X-Forwarded-For获取真实IP

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 重构 ApiKeyAuthInterceptor 成为独立拦截器

**Files:**
- Create: `gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthInterceptor.java`
- Modify: `gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/ApiKeyAuthInterceptorTest.java`

- [ ] **Step 1: 创建 ApiKeyAuthInterceptor 单元测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/ApiKeyAuthInterceptorTest.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.config.satoken.ApiKeyAuthInterceptor;
import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.authentication.UserAuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthInterceptor Tests")
class ApiKeyAuthInterceptorTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ApiKeyAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ApiKeyAuthInterceptor(authenticationService);
    }

    @Test
    @DisplayName("有效API Key通过检查")
    void validApiKey_passes() throws Exception {
        // given
        when(request.getHeader("X-API-Key")).thenReturn("sk-test123");
        when(authenticationService.authenticate("sk-test123"))
                .thenReturn(new UserAuthResult(1L, "user1", "USER", 1L, "key1"));
        when(request.getRequestURI()).thenReturn("/api/test");

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isTrue();
        verify(request).setAttribute("userId", 1L);
        verify(request).setAttribute("userCode", "user1");
        verify(request).setAttribute("apiKeyId", 1L);
    }

    @Test
    @DisplayName("缺少API Key返回401")
    void missingApiKey_returns401() throws Exception {
        // given
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("无效API Key返回401")
    void invalidApiKey_returns401() throws Exception {
        // given
        when(request.getHeader("X-API-Key")).thenReturn("sk-invalid");
        when(authenticationService.authenticate("sk-invalid")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(new MockPrintWriter());

        // when
        boolean result = interceptor.preHandle(request, response);

        // then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("order返回2，在IP检查之后")
    void order_returns2() {
        assertThat(interceptor.order()).isEqualTo(2);
    }

    private static class MockPrintWriter extends java.io.PrintWriter {
        public MockPrintWriter() { super(java.io.OutputStream.nullOutputStream()); }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=ApiKeyAuthInterceptorTest -pl gateway-web`
Expected: FAIL - class not found

- [ ] **Step 3: 创建 ApiKeyAuthInterceptor**

```java
// gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthInterceptor.java
package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.authentication.UserAuthResult;
import com.codingas.gateway.core.security.interceptor.AbstractGatewayInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API Key 认证拦截器
 *
 * <p>责任链第二个拦截器，验证 API Key 并加载用户信息。</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthInterceptor extends AbstractGatewayInterceptor {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String USER_ID_ATTR = "userId";

    private final AuthenticationService authenticationService;

    @Override
    public String name() {
        return "ApiKeyAuth";
    }

    @Override
    public int order() {
        return 2; // IP检查之后
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API Key in request to {}", request.getRequestURI());
            try {
                unauthorized(response, "Missing API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        UserAuthResult userInfo = authenticationService.authenticate(apiKey);
        if (userInfo == null) {
            log.warn("Invalid API Key for request to {}", request.getRequestURI());
            try {
                unauthorized(response, "Invalid API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        // 存储用户信息到请求属性
        request.setAttribute(USER_ID_ATTR, userInfo.userId());
        request.setAttribute("userCode", userInfo.userCode());
        request.setAttribute("apiKeyId", userInfo.apiKeyId());

        log.debug("API Key authenticated: userId={}, keyCode={}",
                userInfo.userId(), userInfo.userCode());
        return true;
    }
}
```

- [ ] **Step 4: 修改 SaTokenConfig 使用 SecurityInterceptorChain**

```java
// gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java
package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.interceptor.SecurityInterceptorChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sa-Token 配置
 *
 * <p>使用 SecurityInterceptorChain 责任链管理多个拦截器。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SecurityInterceptorChain securityInterceptorChain;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityChainInterceptorAdapter(securityInterceptorChain))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/health",
                        "/ready",
                        "/actuator/**",
                        "/error"
                )
                .order(1);
    }

    /**
     * 安全链拦截器适配器
     *
     * <p>将 SecurityInterceptorChain 适配为 Spring HandlerInterceptor。</p>
     */
    @Slf4j
    public static class SecurityChainInterceptorAdapter implements HandlerInterceptor {

        private final SecurityInterceptorChain chain;

        public SecurityChainInterceptorAdapter(SecurityInterceptorChain chain) {
            this.chain = chain;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            try {
                return chain.execute(request, response);
            } catch (Exception e) {
                log.error("Error executing security chain", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;
            }
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest=ApiKeyAuthInterceptorTest -pl gateway-web`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add gateway-application/src/main/java/com/codingas/gateway/config/satoken/ApiKeyAuthInterceptor.java
git add gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/ApiKeyAuthInterceptorTest.java
git commit -m "refactor(security): 重构ApiKeyAuthInterceptor为独立拦截器

- 实现GatewayInterceptor接口
- SaTokenConfig改为使用SecurityInterceptorChain
- 支持拦截器链式组合

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 创建 MaskingResponseAdvice 全局响应脱敏器

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingResponseAdviceTest.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingTestController.java`

- [ ] **Step 1: 创建 MaskingResponseAdvice 单元测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingResponseAdviceTest.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.core.security.masking.SensitiveDataMasker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MaskingTestController.class)
@Import(MaskingResponseAdvice.class)
@DisplayName("MaskingResponseAdvice Tests")
class MaskingResponseAdviceTest {

    @MockBean
    private SensitiveDataMasker sensitiveDataMasker;

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("mask(String)")
    class MaskStringTests {

        @Test
        @DisplayName("对字符串响应进行脱敏")
        void maskStringResponse() throws Exception {
            when(sensitiveDataMasker.mask("13812345678")).thenReturn("138****5678");

            mockMvc.perform(get("/mask-test/string")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("138****5678"));
        }

        @Test
        @DisplayName("空字符串不处理")
        void emptyString_returnsOriginal() throws Exception {
            mockMvc.perform(get("/mask-test/empty")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }
    }

    @Nested
    @DisplayName("不需要脱敏的场景")
    class NoMaskingTests {

        @Test
        @DisplayName("Map 类型响应不进行字符串脱敏")
        void mapResponse_notMasked() throws Exception {
            mockMvc.perform(get("/mask-test/map")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("test"));
        }
    }
}
```

- [ ] **Step 2: 创建 MaskingTestController**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingTestController.java
package com.codingas.gateway.web.advice;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mask-test")
public class MaskingTestController {

    @GetMapping("/string")
    public String maskString() {
        return "13812345678";
    }

    @GetMapping("/empty")
    public String empty() {
        return "";
    }

    @GetMapping("/map")
    public Map<String, String> map() {
        return Map.of("name", "test");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `./mvnw test -Dtest=MaskingResponseAdviceTest -pl gateway-web`
Expected: FAIL - MaskingResponseAdvice not found

- [ ] **Step 4: 创建 MaskingResponseAdvice**

```java
// gateway-web/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.core.security.masking.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseBodyAdvice;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局响应脱敏处理器
 *
 * <p>对所有 REST 响应的字符串内容进行敏感数据脱敏。</p>
 */
@Slf4j
@RestController
public class MaskingResponseAdvice implements ResponseBodyAdvice<Object, Object> {

    private final SensitiveDataMasker sensitiveDataMasker;

    public MaskingResponseAdvice(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends MediaType> converterType) {
        return returnType.getParameterType().equals(String.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpInputMessage> inputMessage,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (!(body instanceof String text)) {
            return body;
        }

        if (text.isEmpty()) {
            return text;
        }

        String masked = sensitiveDataMasker.mask(text);
        log.debug("Sensitive data masked for {}: {} chars -> {} chars",
                request.getURI().getPath(), text.length(), masked.length());

        return masked;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest=MaskingResponseAdviceTest -pl gateway-web`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingResponseAdviceTest.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingTestController.java
git commit -m "feat(security): 添加全局响应敏感数据脱敏

- MaskingResponseAdvice实现ResponseBodyAdvice
- 自动对String类型响应进行脱敏处理

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: Integration Test 和覆盖率验证

- [ ] **Step 1: 创建集成测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/SecurityChainIT.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.config.satoken.IPBlockCheckInterceptor;
import com.codingas.gateway.config.satoken.ApiKeyAuthInterceptor;
import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.authentication.UserAuthResult;
import com.codingas.gateway.core.security.interceptor.SecurityInterceptorChain;
import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SecurityInterceptorChain.class, IPBlockCheckInterceptor.class, ApiKeyAuthInterceptor.class})
@DisplayName("SecurityInterceptorChain Integration Tests")
class SecurityChainIT {

    @MockBean
    private IpBlocklistService ipBlocklistService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("封锁IP返回403")
    void blockedIp_returns403() throws Exception {
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(true);

        mockMvc.perform(get("/health")
                .header("X-API-Key", "sk-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("正常IP和有效Key返回200")
    void normalRequest_returns200() throws Exception {
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(false);
        when(authenticationService.authenticate("sk-valid"))
                .thenReturn(new UserAuthResult(1L, "user1", "USER", 1L, "key1"));

        mockMvc.perform(get("/health")
                .header("X-API-Key", "sk-valid"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("X-Forwarded-For中的封锁IP被正确识别")
    void xForwardedForBlockedIp_returns403() throws Exception {
        when(ipBlocklistService.isBlocked("192.168.1.100")).thenReturn(true);

        mockMvc.perform(get("/health")
                .header("X-Forwarded-For", "192.168.1.100")
                .header("X-API-Key", "sk-test"))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `./mvnw test -Dtest=SecurityChainIT -pl gateway-web`
Expected: PASS

- [ ] **Step 3: 运行完整测试套件**

Run: `./mvnw test -pl gateway-web,gateway-core`
Expected: ALL PASS

- [ ] **Step 4: 检查覆盖率**

Run: `./mvnw test -pl gateway-web -Djacoco`
Expected: Line coverage ≥ 80% for modified classes

- [ ] **Step 5: 提交**

```bash
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/SecurityChainIT.java
git commit -m "test(security): 添加安全拦截器链集成测试

- 测试封锁IP返回403
- 测试正常请求流程
- 测试X-Forwarded-For头解析

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Self-Review Checklist

1. **Spec coverage:** IP 封锁 ✅ / 认证 ✅ / 脱敏 ✅
2. **Placeholder scan:** 无 TBD/TODO ✅
3. **Type consistency:** GatewayInterceptor 接口一致 ✅
4. **Chain order:** IPBlockCheckInterceptor(1) → ApiKeyAuthInterceptor(2) ✅
5. **Test coverage:** 目标 ≥80% ✅

---

**Plan complete.** 请审阅并选择执行方式：
1. **Subagent-Driven (recommended)** - 每个 Task 由独立 subagent 执行，Task 间有检查点
2. **Inline Execution** - 在当前 session 执行，使用 executing-plans 技能
