package com.codingas.gateway.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 安全事件追踪拦截器 (WebFlux 版本)
 *
 * <p>所有安全事件（认证/授权/限流/脱敏）均记录 trace_id。</p>
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityTracingFilter implements WebFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String TRACE_ATTR = "security.trace_id";
    public static final String START_TIME_ATTR = "security.start_time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 获取或生成 trace ID
        String rawTraceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        final String traceId;
        if (rawTraceId == null || rawTraceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        } else {
            traceId = rawTraceId;
        }

        // 设置到响应头
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);

        // 存储到 exchange 属性供后续使用
        exchange.getAttributes().put(TRACE_ATTR, traceId);
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        String clientIp = getClientIp(exchange);

        log.debug("Security trace started: traceId={}, uri={}, clientIp={}",
                traceId, exchange.getRequest().getPath().value(), clientIp);

        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    Long startTime = exchange.getAttribute(START_TIME_ATTR);
                    if (startTime != null) {
                        long duration = System.currentTimeMillis() - startTime;
                        int status = exchange.getResponse().getStatusCode() != null
                                ? exchange.getResponse().getStatusCode().value() : 0;
                        log.debug("Security trace completed: traceId={}, status={}, duration={}ms",
                                traceId, status, duration);
                    }
                })
                .doOnError(ex -> {
                    log.warn("Security request failed: traceId={}, error={}", traceId, ex.getMessage());
                });
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    /**
     * 获取当前请求的 trace ID
     */
    public static String getTraceId(ServerWebExchange exchange) {
        return exchange.getAttribute(TRACE_ATTR);
    }
}
