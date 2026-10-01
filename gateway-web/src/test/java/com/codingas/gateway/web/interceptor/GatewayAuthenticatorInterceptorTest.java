/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
        assertThat(sw.toString()).contains("无效的 API Key");
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
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
        assertThat(sw.toString()).contains("请先登录");
    }

    @Test
    @DisplayName("管理端点会话认证异常 → 401 认证失败（不传播 500）")
    void managedPath_sessionAuthThrows_rejected401() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/channels");
        when(sessionAuthenticationService.authenticateSession())
                .thenThrow(new RuntimeException("invalid token"));
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertThat(interceptor.preHandle(request, response)).isFalse();
        verify(response).setStatus(401);
        assertThat(sw.toString()).contains("认证失败");
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
