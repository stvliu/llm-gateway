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
import com.codingas.gateway.iam.auth.AuthorizationService;
import com.codingas.gateway.iam.auth.Identity;
import com.codingas.gateway.iam.auth.SessionAuthenticationService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 统一认证拦截器
 *
 * <p>替代 {@code ApiKeyAuthInterceptor} 与 {@code TokenAuthInterceptor} 两个拦截器，
 * 按路径分类统一完成认证，并统一产出 {@link Identity} 注入 {@code identity} request attribute：</p>
 * <ul>
 *   <li><b>代理端点</b>（{@code /v1/chat/completions}、{@code /v1/messages}、{@code /v1/models}、
 *       {@code /anthropic/v1/messages}、{@code /anthropic/v1/models}）：API Key 认证
 *       （{@code Authorization: Bearer} 或 {@code x-api-key}）；</li>
 *   <li><b>管理端点</b>（{@code /api/v1/}，除公开路径）：Sa-Token 会话认证；</li>
 *   <li>其余路径放行。</li>
 * </ul>
 *
 * <p>401 语义与 {@code docs/api-spec.md} 一致：代理端点 {@code 无效的 API Key}，管理面 {@code 请先登录}。</p>
 * <p>SSE 异步分发阶段不重复认证：身份已在初始请求注入，直接恢复。</p>
 */
@Slf4j
@Component
public class GatewayAuthenticatorInterceptor extends AbstractGatewayInterceptor {

    /** 代理端点路径前缀（API Key 认证） */
    private static final List<String> PROXY_PATHS = List.of(
            "/v1/chat/completions",
            "/v1/messages",
            "/v1/models",
            "/anthropic/v1/messages",
            "/anthropic/v1/models"
    );

    /** 管理 API 路径前缀（会话认证） */
    private static final String MANAGED_PREFIX = "/api/v1/";

    /** 公开路径（无需认证）——由授权侧 PUBLIC 规则派生（单一事实源） */
    private final Set<String> publicPaths;

    private final AuthenticationService authenticationService;
    private final SessionAuthenticationService sessionAuthenticationService;

    public GatewayAuthenticatorInterceptor(AuthenticationService authenticationService,
                                           SessionAuthenticationService sessionAuthenticationService,
                                           AuthorizationService authorizationService) {
        this.authenticationService = authenticationService;
        this.sessionAuthenticationService = sessionAuthenticationService;
        // 授权服务仅用于构造期派生 publicPaths 快照（单一事实源），不保留实例引用
        this.publicPaths = Set.copyOf(authorizationService.publicPathPatterns());
    }

    @Override
    public String name() {
        return "Authenticator";
    }

    @Override
    public int order() {
        return 1; // IPBlock(0) 之后、Permission(3) 之前
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        // SSE 异步分发：身份已在初始请求注入，恢复即可，不重复认证
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            if (request.getAttribute("identity") == null) {
                log.warn("SSE 异步分发未携带 identity，初始请求可能未认证: path={}", request.getRequestURI());
            }
            return true;
        }

        String path = request.getRequestURI();

        if (isProxyPath(path)) {
            return authenticateApiKey(request, response, path);
        }

        if (path.startsWith(MANAGED_PREFIX)) {
            return publicPaths.contains(path)
                    || authenticateSession(request, response, path);
        }

        return true;
    }

    /** 代理端点：API Key 认证 */
    private boolean authenticateApiKey(HttpServletRequest request, HttpServletResponse response, String path) {
        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("请求缺少 API Key: path={}", path);
            return rejectUnauthorized(response, "无效的 API Key");
        }
        try {
            Identity identity = authenticationService.authenticateUser(apiKey);
            request.setAttribute("identity", identity);
            return true;
        } catch (AuthenticationFailedException e) {
            log.warn("认证失败: path={}, reason={}", path, e.getMessage());
            return rejectUnauthorized(response, "无效的 API Key");
        }
    }

    /** 管理端点：会话认证 */
    private boolean authenticateSession(HttpServletRequest request, HttpServletResponse response, String path) {
        Optional<Identity> identityOpt;
        try {
            identityOpt = sessionAuthenticationService.authenticateSession();
        } catch (Exception e) {
            // 会话校验异常（如无效 token 触发 SaTokenException）不得传播为 500，
            // 与 api-spec 的 401 语义一致：管理面会话缺失/无效 → 401「认证失败」
            log.error("会话认证异常: path={}", path, e);
            return rejectUnauthorized(response, "认证失败");
        }
        if (identityOpt.isEmpty()) {
            log.warn("会话认证失败: path={}", path);
            return rejectUnauthorized(response, "请先登录");
        }
        request.setAttribute("identity", identityOpt.get());
        return true;
    }

    /** 从 Authorization 或 x-api-key header 提取 API Key */
    private String extractApiKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return request.getHeader("x-api-key");
    }

    /** 判断是否为代理端点路径 */
    private boolean isProxyPath(String path) {
        for (String proxyPath : PROXY_PATHS) {
            if (path.startsWith(proxyPath)) {
                return true;
            }
        }
        return false;
    }

    /** 短路 401 */
    private boolean rejectUnauthorized(HttpServletResponse response, String message) {
        try {
            unauthorized(response, message);
        } catch (Exception e) {
            log.error("Failed to write unauthorized response", e);
        }
        return false;
    }
}
