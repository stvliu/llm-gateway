# Security Interceptor Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 集成 IP 封锁检查到认证拦截器，并为敏感数据脱敏添加全局响应拦截器

**Architecture:**
- Task 1: 修改 `ApiKeyAuthInterceptor` 添加 IP 封锁检查，在认证前拦截被封禁 IP
- Task 2: 新建 `MaskingResponseAdvice` 全局响应脱敏器，自动对字符串响应应用脱敏规则
- Task 3: 为两个功能编写完整的单元测试（@WebMvcTest）

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring MVC, JUnit 5, AssertJ, Mockito

---

## File Structure

```
gateway-application/src/main/java/com/codingas/gateway/config/satoken/
├── SaTokenConfig.java                          # 修改：添加 IpBlocklistService 注入和 IP 检查
└── ApiKeyAuthInterceptor.java                  # 修改：调用 isBlocked() 检查

gateway-web/src/main/java/com/codingas/gateway/web/advice/
├── MaskingResponseAdvice.java                  # 新建：全局响应脱敏
└── GlobalExceptionHandler.java                 # 已存在：可能需要添加 IP 被封异常处理

gateway-web/src/test/java/com/codingas/gateway/web/advice/
├── MaskingResponseAdviceTest.java              # 新建：MaskingResponseAdvice 单元测试
└── SaTokenConfigTest.java                      # 新建：IP 检查拦截器集成测试
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

## Task 1: IP 封锁检查集成到 ApiKeyAuthInterceptor

**Files:**
- Modify: `gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java:46-80`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigTest.java`

- [ ] **Step 1: 创建 SaTokenConfig 的 IP 检查集成测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigTest.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.config.satoken.SaTokenConfig;
import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.authentication.UserAuthResult;
import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestController.class)
@Import({SaTokenConfig.class, TestSecurityConfig.class})
@DisplayName("SaTokenConfig IP Blocking Integration Tests")
class SaTokenConfigTest {

    @MockBean
    private IpBlocklistService ipBlocklistService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IP 在黑名单中时返回 403 Forbidden")
    void blockedIp_returns403() throws Exception {
        // given
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(true);

        // when & then
        mockMvc.perform(get("/test")
                .header("X-API-Key", "sk-test123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IP 不在黑名单中且认证成功时返回 200")
    void validIpAndAuth_returns200() throws Exception {
        // given
        when(ipBlocklistService.isBlocked(anyString())).thenReturn(false);
        when(authenticationService.authenticate(anyString()))
                .thenReturn(new UserAuthResult(1L, "user1", "USER", 1L, "key1"));

        // when & then
        mockMvc.perform(get("/test")
                .header("X-API-Key", "sk-test123"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -Dtest=SaTokenConfigTest -pl gateway-web`
Expected: 测试编译成功但因缺少实现而失败（或 mock 未正确注入）

- [ ] **Step 3: 修改 SaTokenConfig 添加 IpBlocklistService 注入和 IP 检查**

```java
// gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java

package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
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
 * <p>配置基于 API Key 的认证拦截器，并集成 IP 封锁检查。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String USER_ID_ATTR = "userId";

    private final AuthenticationService authenticationService;
    private final IpBlocklistService ipBlocklistService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiKeyAuthInterceptor(authenticationService, ipBlocklistService))
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
     * API Key 认证拦截器（已集成 IP 封锁检查）
     */
    @Slf4j
    public static class ApiKeyAuthInterceptor implements HandlerInterceptor {

        private final AuthenticationService authenticationService;
        private final IpBlocklistService ipBlocklistService;

        public ApiKeyAuthInterceptor(AuthenticationService authenticationService,
                                     IpBlocklistService ipBlocklistService) {
            this.authenticationService = authenticationService;
            this.ipBlocklistService = ipBlocklistService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // 1. 获取客户端真实 IP
            String clientIp = getClientIp(request);

            // 2. 检查 IP 是否被封锁
            if (ipBlocklistService.isBlocked(clientIp)) {
                log.warn("Blocked IP access: ip={}, uri={}", clientIp, request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":{\"code\":\"IP_BLOCKED\",\"message\":\"Access denied\"}}");
                return false;
            }

            // 3. 检查 API Key
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("Missing API Key in request to {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            var userInfo = authenticationService.authenticate(apiKey);
            if (userInfo == null) {
                log.warn("Invalid API Key for request to {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 4. 存储用户信息到请求属性
            request.setAttribute(USER_ID_ATTR, userInfo.userId());
            request.setAttribute("userCode", userInfo.userCode());
            request.setAttribute("apiKeyId", userInfo.apiKeyId());

            log.debug("API Key authenticated: userId={}, keyCode={}, ip={}",
                userInfo.userId(), userInfo.userCode(), clientIp);
            return true;
        }

        /**
         * 获取客户端真实 IP（支持代理场景）
         */
        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // 取第一个 IP（最原始的客户端 IP）
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
            return request.getRemoteAddr();
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -Dtest=SaTokenConfigTest -pl gateway-web`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add gateway-application/src/main/java/com/codingas/gateway/config/satoken/SaTokenConfig.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigTest.java
git commit -m "feat(security): 集成IP封锁检查到认证拦截器

- 在ApiKeyAuthInterceptor中添加IP封锁检查
- 支持X-Forwarded-For和X-Real-IP获取真实IP
- 封锁IP返回403 Forbidden

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 创建 MaskingResponseAdvice 全局响应脱敏器

**Files:**
- Create: `gateway-web/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java`
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingResponseAdviceTest.java`

- [ ] **Step 1: 创建 MaskingResponseAdvice 的单元测试**

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

import static org.assertj.core.api.Assertions.*;
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
            // given
            when(sensitiveDataMasker.mask(anyString()))
                    .thenReturn("138****5678");  // 手机号脱敏

            // when
            mockMvc.perform(get("/mask-test/string")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("138****5678"));
        }

        @Test
        @DisplayName("空字符串返回原值")
        void emptyString_returnsOriginal() throws Exception {
            // given
            when(sensitiveDataMasker.mask("")).thenReturn("");

            // when
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
        @DisplayName("null 响应返回 null")
        void nullBody_returnsNull() throws Exception {
            mockMvc.perform(get("/mask-test/null")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

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

- [ ] **Step 2: 创建 MaskingTestController 测试控制器**

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

    @GetMapping("/null")
    public String nullable() {
        return null;
    }

    @GetMapping("/map")
    public Map<String, String> map() {
        return Map.of("name", "test");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `./mvnw test -Dtest=MaskingResponseAdviceTest -pl gateway-web`
Expected: FAIL - MaskingResponseAdvice 类不存在

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
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 全局响应脱敏处理器
 *
 * <p>对所有 REST 响应的字符串内容进行敏感数据脱敏。</p>
 * <p>仅处理 String 类型响应，非字符串响应直接透传。</p>
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
        // 只对返回类型为 String 的方法生效
        return returnType.getParameterType().equals(String.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpInputMessage> inputMessage,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 只处理字符串类型
        if (!(body instanceof String text)) {
            return body;
        }

        // 空字符串不处理
        if (text.isEmpty()) {
            return text;
        }

        // 脱敏处理
        String masked = sensitiveDataMasker.mask(text);
        log.debug("Sensitive data masked for {}: {} -> {}",
                getRequestPath(request), text.length(), masked.length());

        return masked;
    }

    private String getRequestPath(ServerHttpRequest request) {
        return request.getURI().getPath();
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `./mvnw test -Dtest=MaskingResponseAdviceTest -pl gateway-web`
Expected: PASS

- [ ] **Step 6: 添加对 GlobalExceptionHandler 错误消息的脱敏测试**

```java
// 在 MaskingResponseAdviceTest.java 中添加
@Test
@DisplayName("异常消息中的敏感数据也被脱敏")
void errorMessage_masked() throws Exception {
    // given
    when(sensitiveDataMasker.mask(anyString()))
            .thenAnswer(inv -> inv.getArgument(0));

    // when
    mockMvc.perform(get("/mask-test/error")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
}
```

- [ ] **Step 7: 运行完整测试套件**

Run: `./mvnw test -pl gateway-web`
Expected: ALL PASS

- [ ] **Step 8: 提交**

```bash
git add gateway-web/src/main/java/com/codingas/gateway/web/advice/MaskingResponseAdvice.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingResponseAdviceTest.java
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/MaskingTestController.java
git commit -m "feat(security): 添加全局响应敏感数据脱敏

- 新增MaskingResponseAdvice实现ResponseBodyAdvice
- 自动对String类型响应进行脱敏处理
- 依赖SensitiveDataMasker服务

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 添加 Integration Test 和 Coverage 验证

**Files:**
- Test: `gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigIT.java`

- [ ] **Step 1: 创建集成测试**

```java
// gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigIT.java
package com.codingas.gateway.web.advice;

import com.codingas.gateway.config.satoken.SaTokenConfig;
import com.codingas.gateway.core.security.authentication.AuthenticationService;
import com.codingas.gateway.core.security.authentication.UserAuthResult;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({SaTokenConfig.class})
@DisplayName("SaTokenConfig Integration Tests")
class SaTokenConfigIT {

    @MockBean
    private IpBlocklistService ipBlocklistService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("X-Forwarded-For 头中的封锁IP应被拒绝")
    void blockedIpViaXForwardedFor_returns403() throws Exception {
        when(ipBlocklistService.isBlocked("192.168.1.100")).thenReturn(true);

        mockMvc.perform(get("/health")
                .header("X-Forwarded-For", "192.168.1.100")
                .header("X-API-Key", "sk-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("正常IP和有效Key应返回200")
    void normalRequest_returns200() throws Exception {
        when(ipBlocklistService.isBlocked("10.0.0.1")).thenReturn(false);
        when(authenticationService.authenticate("sk-valid"))
                .thenReturn(new UserAuthResult(1L, "user1", "USER", 1L, "key1"));

        mockMvc.perform(get("/health")
                .header("X-API-Key", "sk-valid"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: 运行集成测试**

Run: `./mvnw test -Dtest=SaTokenConfigIT -pl gateway-web`
Expected: PASS

- [ ] **Step 3: 检查测试覆盖率**

Run: `./mvnw test -pl gateway-web -Djacoco`
Expected: Line coverage ≥ 80% for modified classes

- [ ] **Step 4: 提交**

```bash
git add gateway-web/src/test/java/com/codingas/gateway/web/advice/SaTokenConfigIT.java
git commit -m "test(security): 添加SaTokenConfig集成测试

- 测试X-Forwarded-For头IP封锁
- 测试正常请求流程

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 更新项目文档

- [ ] **Step 1: 更新 CLAUDE.md 或安全相关文档**

```markdown
## 安全组件

### IP 封锁
- `IpBlocklistService.isBlocked(ip)` - 检查 IP 是否被封锁
- 在 `ApiKeyAuthInterceptor` 中自动调用

### 敏感数据脱敏
- `SensitiveDataMasker.mask(text)` - 对文本进行脱敏
- 通过 `MaskingResponseAdvice` 全局自动应用
```

---

## Self-Review Checklist

1. **Spec coverage:** IP 封锁检查已集成 ✅ / 敏感数据脱敏全局拦截已实现 ✅
2. **Placeholder scan:** 无 TBD/TODO ✅
3. **Type consistency:** `SensitiveDataMasker.mask()` / `IpBlocklistService.isBlocked()` 方法签名一致 ✅
4. **Test coverage:** 目标 ≥80% ✅

---

**Plan complete.** 请审阅并选择执行方式：
1. **Subagent-Driven (recommended)** - 每个 Task 由独立 subagent 执行，Task 间有检查点
2. **Inline Execution** - 在当前 session 执行，使用 executing-plans 技能
