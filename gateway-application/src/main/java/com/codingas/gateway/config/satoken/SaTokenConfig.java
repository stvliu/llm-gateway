package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Sa-Token 配置
 *
 * <p>配置基于 API Key 的认证拦截器。</p>
 * <p>Note: 当前为简化实现，完整 Sa-Token 集成需要在 pom.xml 中添加依赖。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String USER_ID_ATTR = "userId";

    private final AuthenticationService authenticationService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiKeyAuthInterceptor(authenticationService))
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/health",
                "/ready",
                "/actuator/**",
                "/error"
            )
            .order(1);
    }

    /**
     * API Key 认证拦截器
     */
    @Slf4j
    public static class ApiKeyAuthInterceptor implements HandlerInterceptor {

        private final AuthenticationService authenticationService;

        public ApiKeyAuthInterceptor(AuthenticationService authenticationService) {
            this.authenticationService = authenticationService;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey == null || apiKey.isBlank()) {
                log.warn("Missing API Key in request to {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            var userInfo = authenticationService.authenticate(apiKey);
            if (userInfo == null) {
                log.warn("Invalid API Key for request to {}", request.getRequestURI());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 存储用户信息到请求属性 (Record 使用 `userInfo.userId()` 语法)
            request.setAttribute(USER_ID_ATTR, userInfo.userId());
            request.setAttribute("userCode", userInfo.userCode());
            request.setAttribute("apiKeyId", userInfo.apiKeyId());

            log.debug("API Key authenticated: userId={}, keyCode={}", userInfo.userId(), userInfo.apiKeyCode());
            return true;
        }
    }
}
