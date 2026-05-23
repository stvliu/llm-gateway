package com.codingas.gateway.adapter.interceptor;

import com.codingas.gateway.domain.iam.service.Identity;
import com.codingas.gateway.domain.threat.service.RateLimitDomainService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 限流拦截器
 *
 * <p>基于 API Key 级别的令牌桶限流，order=1 在 IP 封禁之后执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor extends AbstractGatewayInterceptor {

    private final RateLimitDomainService rateLimitDomainService;

    @Override
    public String name() {
        return "RateLimit";
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        if (!isProxyPath(path)) {
            return true;
        }

        Identity identity = (Identity) request.getAttribute("identity");
        if (identity == null || identity.credentialId() == null) {
            return true; // 尚未认证，放行给后续认证拦截器处理
        }

        if (!rateLimitDomainService.isAllowed(identity.credentialId())) {
            log.warn("Rate limit exceeded: credentialId={}, path={}", identity.credentialId(), path);
            try {
                response.setStatus(429);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"请求过于频繁，请稍后重试\"}}");
            } catch (Exception e) {
                log.error("Failed to write rate limit response", e);
            }
            return false;
        }

        return true;
    }

    private boolean isProxyPath(String path) {
        return path.startsWith("/v1/chat/completions")
                || path.startsWith("/v1/messages")
                || path.startsWith("/v1/models");
    }
}
