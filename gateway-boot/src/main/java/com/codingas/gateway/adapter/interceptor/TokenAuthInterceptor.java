package com.codingas.gateway.adapter.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Token 认证拦截器
 *
 * <p>责任链第一个拦截器，验证 SaToken 登录状态。</p>
 * <p>用于前端管理界面的用户会话认证。</p>
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
        return 1; // 最先执行
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
}
