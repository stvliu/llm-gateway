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
