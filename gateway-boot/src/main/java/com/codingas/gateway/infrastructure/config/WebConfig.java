package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.adapter.interceptor.SecurityInterceptorChain;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 *
 * <p>配置拦截器和其他 Web 相关设置。</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(ActuatorHealthProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final SecurityInterceptorChain securityInterceptorChain;
    private final ActuatorHealthProperties actuatorHealthProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        var registration = registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     Object handler) throws Exception {
                return securityInterceptorChain.execute(request, response);
            }
        }).addPathPatterns("/api/**", "/v1/**", "/actuator/health/**");

        if (actuatorHealthProperties.isPublicAccess()) {
            registration.excludePathPatterns("/actuator/health/**");
        }
    }

    /**
     * SPA 路由支持
     *
     * <p>将非 API、非静态资源的请求转发到 index.html，让前端路由处理。</p>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requestedResource = location.createRelative(resourcePath);

                    // 如果请求的资源存在，直接返回
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }

                    // API 请求不转发
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("v1/") ||
                        resourcePath.startsWith("actuator/")) {
                        return null;
                    }

                    // 其他请求转发到 index.html（SPA 路由）
                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
