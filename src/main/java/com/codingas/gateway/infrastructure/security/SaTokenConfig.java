package com.codingas.gateway.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Sa-Token WebFlux 配置
 *
 * <p>使用 WebFilter 替代 HandlerInterceptor 实现安全拦截链。</p>
 */
@Slf4j
@Configuration
public class SaTokenConfig {

    private final List<SecurityFilterChain> filterChains;

    public SaTokenConfig(List<SecurityFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    /**
     * 安全链过滤器
     */
    @Bean
    public WebFilter securityChainFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (shouldSkip(path)) {
                return chain.filter(exchange);
            }

            for (SecurityFilterChain filterChain : filterChains) {
                if (!filterChain.filter(exchange)) {
                    return exchange.getResponse().setComplete();
                }
            }

            return chain.filter(exchange);
        };
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/health") ||
               path.startsWith("/ready") ||
               path.startsWith("/actuator") ||
               path.equals("/error");
    }

    /**
     * 安全过滤器链接口
     */
    public interface SecurityFilterChain {
        boolean filter(ServerWebExchange exchange);
    }
}
