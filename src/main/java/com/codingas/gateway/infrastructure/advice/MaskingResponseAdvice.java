package com.codingas.gateway.infrastructure.advice;

import com.codingas.gateway.domain.security.service.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 响应数据脱敏处理 (WebFlux 版本)
 *
 * <p>对响应体中的敏感数据进行脱敏处理。</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MaskingResponseAdvice implements WebFilter {

    private final SensitiveDataMasker sensitiveDataMasker;

    public MaskingResponseAdvice(SensitiveDataMasker sensitiveDataMasker) {
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!shouldMask(path)) {
            return chain.filter(exchange);
        }

        return chain.filter(exchange);
    }

    private boolean shouldMask(String path) {
        return path.startsWith("/v1/") || path.startsWith("/anthropic/v1/");
    }
}
