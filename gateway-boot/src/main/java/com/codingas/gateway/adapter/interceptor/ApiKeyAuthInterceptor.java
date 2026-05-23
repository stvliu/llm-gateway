package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.security.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.security.service.AuthenticationDomainService;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API Key 认证拦截器
 */
@Component
public class ApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthInterceptor.class);

    private final AuthenticationDomainService authenticationDomainService;

    public ApiKeyAuthInterceptor(AuthenticationDomainService authenticationDomainService) {
        this.authenticationDomainService = authenticationDomainService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // 跳过非代理路径
        if (!isProxyPath(path)) {
            return true;
        }

        String apiKey = extractApiKey(request);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("请求缺少 API Key: path={}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            UserAuthResult authResult = authenticationDomainService.authenticateUser(apiKey);
            request.setAttribute("authResult", authResult);
            return true;
        } catch (AuthenticationFailedException e) {
            log.warn("认证失败: path={}, reason={}", path, e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
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
                || path.startsWith("/v1/models");
    }
}
