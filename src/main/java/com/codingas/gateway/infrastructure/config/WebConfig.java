package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.adapter.interceptor.SecurityInterceptorChain;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * <p>配置拦截器和其他 Web 相关设置。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptorChain securityInterceptorChain;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     Object handler) throws Exception {
                return securityInterceptorChain.execute(request, response);
            }
        }).addPathPatterns("/v1/**");
    }
}
