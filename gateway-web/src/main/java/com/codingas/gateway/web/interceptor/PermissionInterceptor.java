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
import com.codingas.gateway.iam.auth.RolePermissions;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 角色授权拦截器
 *
 * <p>基于 USER/ADMIN 两角色授权（users.role 为唯一事实源，与前端角色判断保持一致）：</p>
 * <ul>
 *   <li><b>ADMIN</b>：全部管理端点放行；</li>
 *   <li><b>USER</b>：仅放行白名单（模型/应用只读、体验中心、自己的 API Key，归属由 Service 层 owner check 兜底）；</li>
 *   <li>其他/未登录角色：管理端点默认拒绝（403）。</li>
 * </ul>
 *
 * <p>公开路径（登录）与登录即可路径（个人认证）直接放行；
 * /v1/ API Key 网关路径与非管理路径跳过。</p>
 */
@Slf4j
@Component
public class PermissionInterceptor extends AbstractGatewayInterceptor {

    /** 管理 API 路径前缀（需授权校验） */
    private static final String MANAGED_PREFIX = "/api/v1/";
    /** API Key 认证路径前缀（网关代理端点，由 ApiKeyAuthInterceptor 处理） */
    private static final String API_KEY_PREFIX = "/v1/";

    /** 公开路径（无需登录）：登录接口 */
    private static final List<String> PUBLIC_RULES = List.of(
            "POST /api/v1/auth/login"
    );

    /** 登录即可路径（已通过 TokenAuth 认证，无需额外角色） */
    private static final List<String> LOGIN_ONLY_RULES = List.of(
            "POST /api/v1/auth/logout",
            "GET /api/v1/auth/me",
            "PATCH /api/v1/auth/me/password",
            "GET /api/v1/me/**",
            "GET /api/v1/protocols"
    );

    /**
     * USER 角色白名单（普通用户 / 开发者可用）
     *
     * <p>与 {@link RolePermissions} USER 权限码语义一致；
     * 管理向资源（用户/渠道/供应商/目录/开通/用量/统计/韧性/应用写）不在白名单内，默认拒绝。
     * API Key 仅单条（{@code /*}）放行，列表 findAll 不在白名单（前置拒绝）。</p>
     */
    private static final List<String> USER_ALLOWED_RULES = List.of(
            "GET /api/v1/models/**",
            "GET /api/v1/applications/**",
            "GET /api/v1/experience/**",
            "POST /api/v1/experience/**",
            "GET /api/v1/user-api-keys/*",
            "GET /api/v1/user-api-keys/*/*",
            "POST /api/v1/user-api-keys",
            "PUT /api/v1/user-api-keys/*",
            "DELETE /api/v1/user-api-keys/*"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Override
    public String name() {
        return "Permission";
    }

    @Override
    public int order() {
        return 3; // 在 TokenAuth(order=2) 之后执行
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // SSE 异步分发阶段跳过（授权已在初始请求校验）
        if (isAsyncDispatch(request)) {
            return true;
        }

        String uri = request.getRequestURI();

        // API Key 网关路径（/v1/...），由 ApiKeyAuthInterceptor 处理
        if (uri.startsWith(API_KEY_PREFIX)) {
            return true;
        }

        // 非管理 API 路径（静态资源等），跳过
        if (!uri.startsWith(MANAGED_PREFIX)) {
            return true;
        }

        String method = request.getMethod();

        // 公开路径：无需登录
        if (matches(PUBLIC_RULES, method, uri)) {
            return true;
        }

        // 登录即可路径：TokenAuth 已保证登录态
        if (matches(LOGIN_ONLY_RULES, method, uri)) {
            return true;
        }

        // 角色授权：ADMIN 全通；USER 仅白名单
        if (StpUtil.hasRole(RolePermissions.ROLE_ADMIN)) {
            return true;
        }
        if (StpUtil.hasRole(RolePermissions.ROLE_USER) && matches(USER_ALLOWED_RULES, method, uri)) {
            return true;
        }

        log.warn("角色权限不足: {} {} 被拒绝", method, uri);
        return rejectForbidden(response, "无访问权限");
    }

    /**
     * 按 方法 + 路径 匹配规则（Ant 风格路径模式）
     */
    private boolean matches(List<String> rules, String method, String path) {
        for (String rule : rules) {
            int space = rule.indexOf(' ');
            if (space < 0 || !rule.substring(0, space).equals(method)) {
                continue;
            }
            if (MATCHER.match(rule.substring(space + 1), path)) {
                return true;
            }
        }
        return false;
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

    /**
     * 检测是否为异步分发请求（SSE 等）
     */
    private boolean isAsyncDispatch(HttpServletRequest request) {
        return request.getDispatcherType() == DispatcherType.ASYNC;
    }
}
