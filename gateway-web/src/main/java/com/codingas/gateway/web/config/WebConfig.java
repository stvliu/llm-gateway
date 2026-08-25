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
package com.codingas.gateway.web.config;

import com.codingas.gateway.web.interceptor.SecurityInterceptorChain;
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
 * Web MVC 配置（gateway-web 承载层）
 *
 * <p>注册安全拦截器链与 SPA 静态资源路由。承载层自持相关
 * {@link ActuatorHealthProperties}/{@link CorsProperties} 配置。</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({ActuatorHealthProperties.class, CorsProperties.class})
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
        }).addPathPatterns("/api/**", "/v1/**", "/anthropic/**", "/actuator/health/**");

        if (actuatorHealthProperties.isPublicAccess()) {
            registration.excludePathPatterns("/actuator/health/**");
        }
    }

    /**
     * SPA 路由支持
     *
     * <p>将非 API、非 Actuator、非静态资源的请求转发到 index.html。</p>
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

                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    }

                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("v1/") ||
                        resourcePath.startsWith("actuator/")) {
                        return null;
                    }

                    return new ClassPathResource("/static/index.html");
                }
            });
    }
}
