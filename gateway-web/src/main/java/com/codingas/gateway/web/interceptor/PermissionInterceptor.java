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
