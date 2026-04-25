package com.codingas.gateway.config.satoken;

import com.codingas.gateway.core.security.interceptor.SecurityInterceptorChain;
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
 * <p>使用 SecurityInterceptorChain 责任链管理多个拦截器。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final SecurityInterceptorChain securityInterceptorChain;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SecurityChainInterceptorAdapter(securityInterceptorChain))
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
     * 安全链拦截器适配器
     *
     * <p>将 SecurityInterceptorChain 适配为 Spring HandlerInterceptor。</p>
     */
    @Slf4j
    public static class SecurityChainInterceptorAdapter implements HandlerInterceptor {

        private final SecurityInterceptorChain chain;

        public SecurityChainInterceptorAdapter(SecurityInterceptorChain chain) {
            this.chain = chain;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            try {
                return chain.execute(request, response);
            } catch (Exception e) {
                log.error("Error executing security chain", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return false;
            }
        }
    }
}
