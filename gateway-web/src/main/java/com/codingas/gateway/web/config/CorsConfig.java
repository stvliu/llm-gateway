package com.codingas.gateway.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 */
@Configuration
public class CorsConfig {

    private final GatewayProperties gatewayProperties;

    public CorsConfig(GatewayProperties gatewayProperties) {
        this.gatewayProperties = gatewayProperties;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        GatewayProperties.CorsProperties cors = gatewayProperties.getCors();

        config.setAllowedOriginPatterns(java.util.Arrays.asList(cors.getAllowedOrigins().split(",")));
        config.setAllowedMethods(java.util.Arrays.asList(cors.getAllowedMethods().split(",")));
        config.setAllowedHeaders(java.util.Arrays.asList(cors.getAllowedHeaders().split(",")));
        config.setAllowCredentials(cors.isAllowCredentials());
        config.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
