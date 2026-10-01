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

import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.iam.auth.Identity;
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

    @Test
    @DisplayName("SSE 异步分发跳过授权（授权已在初始请求校验）")
    void asyncDispatch_skips() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getDispatcherType()).thenReturn(DispatcherType.ASYNC);

        assertThat(interceptor.preHandle(req, response())).isTrue();
        verifyNoInteractions(authorizationService);
    }
}
