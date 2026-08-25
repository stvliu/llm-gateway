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

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
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

    private HttpServletRequest request(String method, String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        // lenient：跳过场景（/v1/ 或非管理路径）不读取 method
        lenient().when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        return req;
    }

    private HttpServletResponse response() throws Exception {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        // lenient：放行场景不写响应，避免 UnnecessaryStubbing
        lenient().when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        return resp;
    }

    /** ADMIN 角色上下文 */
    private MockedStatic<StpUtil> stubRole(String role) {
        MockedStatic<StpUtil> stp = mockStatic(StpUtil.class);
        stp.when(() -> StpUtil.hasRole("ADMIN")).thenReturn("ADMIN".equals(role));
        stp.when(() -> StpUtil.hasRole("USER")).thenReturn("USER".equals(role));
        return stp;
    }

    @Test
    @DisplayName("公开路径（登录接口）放行，不校验角色")
    void publicPath_passes() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole(null)) {
            assertThat(interceptor.preHandle(request("POST", "/api/v1/auth/login"), response())).isTrue();
            stp.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("登录即可路径（auth/me）放行，不校验角色")
    void loginOnlyPath_passes() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole(null)) {
            assertThat(interceptor.preHandle(request("GET", "/api/v1/auth/me"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/me/api-keys"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/protocols"), response())).isTrue();
            stp.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("ADMIN 访问管理端点（用户管理/渠道/开通）放行")
    void admin_manageEndpoints_pass() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole("ADMIN")) {
            assertThat(interceptor.preHandle(request("DELETE", "/api/v1/users/1"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/channels"), response())).isTrue();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/provision/from-plan/P1"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/stats"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/token-limits"), response())).isTrue();
        }
    }

    @Test
    @DisplayName("USER 访问白名单（模型读/体验/自己的 Key）放行")
    void user_allowedPaths_pass() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole("USER")) {
            assertThat(interceptor.preHandle(request("GET", "/api/v1/models"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/applications"), response())).isTrue();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/experience/chat"), response())).isTrue();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/user-api-keys/1/detail"), response())).isTrue();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/user-api-keys"), response())).isTrue();
        }
    }

    @Test
    @DisplayName("USER 访问管理端点 → 403 拒绝")
    void user_manageEndpoints_rejected403() throws Exception {
        HttpServletResponse resp = response();
        try (MockedStatic<StpUtil> stp = stubRole("USER")) {
            assertThat(interceptor.preHandle(request("DELETE", "/api/v1/users/1"), resp)).isFalse();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/channels"), response())).isFalse();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/providers"), response())).isFalse();
            assertThat(interceptor.preHandle(request("GET", "/api/v1/stats"), response())).isFalse();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/token-limits"), response())).isFalse();
            assertThat(interceptor.preHandle(request("POST", "/api/v1/applications"), response())).isFalse();
        }
        verify(resp).setStatus(403);
    }

    @Test
    @DisplayName("未授权角色访问管理端点 → 403 拒绝")
    void unknownRole_manageEndpoints_rejected403() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole(null)) {
            assertThat(interceptor.preHandle(request("GET", "/api/v1/models"), response())).isFalse();
            // hasRole 均返回 false（非 ADMIN 亦非 USER），默认拒绝
            stp.verify(() -> StpUtil.hasRole("ADMIN"));
        }
    }

    @Test
    @DisplayName("API Key 网关路径（/v1/）跳过授权")
    void apiKeyPath_skips() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole(null)) {
            assertThat(interceptor.preHandle(request("POST", "/v1/chat/completions"), response())).isTrue();
            stp.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("非管理路径（静态资源等）跳过")
    void nonManagedPath_skips() throws Exception {
        try (MockedStatic<StpUtil> stp = stubRole(null)) {
            assertThat(interceptor.preHandle(request("GET", "/assets/app.js"), response())).isTrue();
            stp.verifyNoInteractions();
        }
    }
}
