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
package com.codingas.gateway.resilience.circuitbreaker;

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

    /**
     * 查询端点熔断器当前状态
     *
     * @param endpointId 端点 ID
     * @return 熔断器状态（CLOSED/OPEN/HALF_OPEN）
     */
    public CircuitBreakerState getState(Long endpointId) {
        return getBreaker(endpointId).getState();
    }

    /**
     * 应急强制熔断端点
     *
     * <p>运维一键熔断：直接将端点熔断器置为 OPEN，立即切断该端点流量。
     * 用于故障应急时快速隔离问题端点。</p>
     *
     * @param endpointId 端点 ID
     */
    public void forceOpen(Long endpointId) {
        getBreaker(endpointId).forceOpen();
    }

    /**
     * 应急强制恢复端点
     *
     * <p>运维一键恢复：将端点熔断器置为 CLOSED 并重置统计窗口，
     * 用于故障修复后立即恢复端点流量。</p>
     *
     * @param endpointId 端点 ID
     */
    public void forceClose(Long endpointId) {
        getBreaker(endpointId).forceClose();
    }
}