package com.codingas.gateway.infrastructure.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 渠道端点熔断器管理器
 *
 * <p>每个 ChannelEndpoint 维护一个独立的熔断器实例。</p>
 */
@Component
public class ChannelEndpointCircuitBreakerManager {

    private final ConcurrentMap<Long, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    private static final double DEFAULT_FAILURE_RATE_THRESHOLD = 0.5;
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 10;
    private static final long DEFAULT_OPEN_DURATION_MS = 30000;
    private static final int DEFAULT_HALF_OPEN_MAX_ATTEMPTS = 3;

    /**
     * 获取端点对应的熔断器
     */
    public CircuitBreaker getBreaker(Long endpointId) {
        return breakers.computeIfAbsent(endpointId,
                id -> new CircuitBreaker(DEFAULT_FAILURE_RATE_THRESHOLD, DEFAULT_SLIDING_WINDOW_SIZE,
                        DEFAULT_OPEN_DURATION_MS, DEFAULT_HALF_OPEN_MAX_ATTEMPTS));
    }

    /**
     * 判断端点是否可用（熔断器非 OPEN 状态）
     */
    public boolean isAvailable(Long endpointId) {
        return getBreaker(endpointId).allowRequest();
    }
}