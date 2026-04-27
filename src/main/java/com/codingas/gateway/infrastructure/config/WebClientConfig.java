package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 配置类
 *
 * <p>提供共享的 WebClient 实例。</p>
 */
@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * 创建共享 WebClient 实例
     */
    @Bean
    public WebClient webClient() {
        log.info("WebClient configured with default settings");
        return WebClient.builder().build();
    }
}
