package com.codingas.gateway.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public Object openAPI() {
        // OpenAPI 配置暂时禁用，需要添加 springdoc-openapi 依赖
        return null;
    }
}
