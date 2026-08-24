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
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Token 认证拦截器
 *
 * <p>责任链第一个拦截器，验证 SaToken 登录状态。</p>
 * <p>用于前端管理界面的用户会话认证。</p>
 * <p>对于 SSE 异步请求，在异步分发阶段跳过认证检查，因为认证信息已在初始请求时验证。</p>
 */
@Slf4j
@Component
public class TokenAuthInterceptor extends AbstractGatewayInterceptor {

    /** 需要 Token 认证的路径前缀 */
    private static final String TOKEN_AUTH_PREFIX = "/api/v1/";

    /** 不需要 Token 认证的路径 */
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/logout"
    };

    /** API Key 认证的路径（由 ApiKeyAuthInterceptor 处理） */
    private static final String API_KEY_PATH = "/v1/";

    @Override
    public String name() {
        return "TokenAuth";
    }

    @Override
    public int order() {
        return 2; // 在限流之后执行
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();

        // API Key 认证路径，跳过 Token 认证
        if (requestURI.startsWith(API_KEY_PATH)) {
            log.debug("API Key path, skipping token auth: {}", requestURI);
            return true;
        }

        // 公开路径不需要认证
        for (String publicPath : PUBLIC_PATHS) {
            if (publicPath.equals(requestURI)) {
                log.debug("Public path, skipping token auth: {}", requestURI);
                return true;
            }
        }

        // 非管理 API 路径，跳过
        if (!requestURI.startsWith(TOKEN_AUTH_PREFIX)) {
            return true;
        }

        // SSE 异步分发阶段，跳过认证检查
        // 初始请求已验证，异步分发时 Sa-Token 上下文不可用
        if (isAsyncDispatch(request)) {
            log.debug("Async dispatch detected, skipping token auth: {}", requestURI);
            // 从初始请求中恢复 userId（已在初始请求中设置）
            Object userId = request.getAttribute("userId");
            if (userId == null) {
                log.warn("Async dispatch without userId attribute, request may not be properly authenticated");
            }
            return true;
        }

        // 验证 SaToken 登录状态
        try {
            if (StpUtil.isLogin()) {
                Long userId = StpUtil.getLoginIdAsLong();
                request.setAttribute("userId", userId);
                log.debug("Token authenticated: userId={}", userId);
                return true;
            } else {
                log.warn("Token not valid for request to {}", requestURI);
                try {
                    unauthorized(response, "请先登录");
                } catch (Exception e) {
                    log.error("Failed to write unauthorized response", e);
                }
                return false;
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
            try {
                unauthorized(response, "认证失败");
            } catch (Exception ex) {
                log.error("Failed to write unauthorized response", ex);
            }
            return false;
        }
    }

    /**
     * 检测是否为异步分发请求
     *
     * <p>SSE 流式响应使用异步处理，Tomcat 会在异步分发时再次调用拦截器。</p>
     * <p>此时 Sa-Token 的 ThreadLocal 上下文已不可用，需要跳过认证检查。</p>
     *
     * @param request HTTP 请求
     * @return true 如果是异步分发
     */
    private boolean isAsyncDispatch(HttpServletRequest request) {
        // 方式 1：检查 DispatcherType
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }

        // 方式 2：检查是否存在 AsyncContext（表示请求已进入异步模式）
        // 注意：在 ASYNC 分发时，AsyncContext 可能已被重新创建
        // 所以主要依赖 DispatcherType 检测

        return false;
    }
}
