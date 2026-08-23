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
package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.auth.AuthenticationDomainService;
import com.codingas.gateway.iam.valueobject.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * API Key 认证拦截器
 *
 * <p>从请求中提取 API Key 并调用领域服务完成认证，order=3 在限流和 Token 认证之后执行。</p>
 */
@Slf4j
@Component
public class ApiKeyAuthInterceptor extends AbstractGatewayInterceptor {

    private final AuthenticationDomainService authenticationDomainService;

    public ApiKeyAuthInterceptor(AuthenticationDomainService authenticationDomainService) {
        this.authenticationDomainService = authenticationDomainService;
    }

    @Override
    public String name() {
        return "ApiKeyAuth";
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();

        // 跳过非代理路径
        if (!isProxyPath(path)) {
            return true;
        }

        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("请求缺少 API Key: path={}", path);
            try {
                unauthorized(response, "无效的 API Key");
            } catch (Exception e) {
                log.error("Failed to write unauthorized response", e);
            }
            return false;
        }

        try {
            Identity identity = authenticationDomainService.authenticateUser(apiKey);
            request.setAttribute("identity", identity);
            return true;
        } catch (AuthenticationFailedException e) {
            log.warn("认证失败: path={}, reason={}", path, e.getMessage());
            try {
                unauthorized(response, "无效的 API Key");
            } catch (Exception ex) {
                log.error("Failed to write unauthorized response", ex);
            }
            return false;
        }
    }

    /** 从 Authorization header 提取 API Key */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getHeader("x-api-key");
    }

    /** 判断是否为代理路径 */
    private boolean isProxyPath(String path) {
        return path.startsWith("/v1/chat/completions")
                || path.startsWith("/v1/messages")
                || path.startsWith("/v1/models")
                || path.startsWith("/anthropic/v1/messages")
                || path.startsWith("/anthropic/v1/models");
    }
}
