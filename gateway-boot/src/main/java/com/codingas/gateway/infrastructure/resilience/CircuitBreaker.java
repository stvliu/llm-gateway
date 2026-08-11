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
package com.codingas.gateway.infrastructure.resilience;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器
 *
 * <p>基于滑动窗口统计失败率，支持 CLOSED→OPEN→HALF_OPEN 状态转换。</p>
 * <p>状态转换使用 synchronized 保护，避免并发下的竞态条件。</p>
 */
public class CircuitBreaker {

    private final double failureRateThreshold;
    private final int slidingWindowSize;
    private final long openDurationMs;
    private final int halfOpenMaxAttempts;

    private final ConcurrentLinkedDeque<Boolean> slidingWindow = new ConcurrentLinkedDeque<>();
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private volatile long openSince = 0;
    private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);

    public CircuitBreaker(double failureRateThreshold, int slidingWindowSize,
                          long openDurationMs, int halfOpenMaxAttempts) {
        this.failureRateThreshold = failureRateThreshold;
        this.slidingWindowSize = slidingWindowSize;
        this.openDurationMs = openDurationMs;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    /**
     * 判断是否允许请求通过
     */
    public boolean allowRequest() {
        return switch (state) {
            case CLOSED -> true;
            case OPEN -> {
                synchronized (this) {
                    if (state == CircuitBreakerState.OPEN
                            && System.currentTimeMillis() - openSince >= openDurationMs) {
                        state = CircuitBreakerState.HALF_OPEN;
                        halfOpenAttempts.set(0);
                        yield true;
                    }
                }
                yield state != CircuitBreakerState.OPEN;
            }
            case HALF_OPEN -> halfOpenAttempts.incrementAndGet() <= halfOpenMaxAttempts;
        };
    }

    /**
     * 记录成功
     */
    public void recordSuccess() {
        recordAndEvaluate(true);
    }

    /**
     * 记录失败
     */
    public void recordFailure() {
        recordAndEvaluate(false);
    }

    public CircuitBreakerState getState() { return state; }

    /**
     * 应急强制熔断
     *
     * <p>运维一键熔断：无视滑动窗口统计直接将状态置为 OPEN，用于故障应急时
     * 立即切断端点流量。与 {@link #recordFailure()} 的被动熔断不同，
     * forceOpen 是管理员主动操作，不依赖失败率阈值。</p>
     */
    public void forceOpen() {
        synchronized (this) {
            tripOpen();
        }
    }

    /**
     * 应急强制恢复
     *
     * <p>运维一键恢复：将状态置为 CLOSED 并重置滑动窗口与失败计数，
     * 用于故障修复后立即恢复端点流量。重置窗口避免历史失败记录
     * 导致恢复后立即再次熔断。</p>
     */
    public void forceClose() {
        synchronized (this) {
            state = CircuitBreakerState.CLOSED;
            slidingWindow.clear();
            failureCount.set(0);
            halfOpenAttempts.set(0);
            openSince = 0;
        }
    }

    private void recordAndEvaluate(boolean success) {
        record(success);
        synchronized (this) {
            if (state == CircuitBreakerState.HALF_OPEN) {
                if (success) {
                    state = CircuitBreakerState.CLOSED;
                    slidingWindow.clear();
                    failureCount.set(0);
                } else {
                    tripOpen();
                }
            } else if (state == CircuitBreakerState.CLOSED && shouldTripOpen()) {
                tripOpen();
            }
        }
    }

    private void record(boolean success) {
        Boolean removed = null;
        slidingWindow.addLast(success);
        while (slidingWindow.size() > slidingWindowSize) {
            removed = slidingWindow.pollFirst();
        }
        if (!success) {
            failureCount.incrementAndGet();
        }
        if (removed != null && !removed) {
            failureCount.decrementAndGet();
        }
    }

    /**
     * 判断是否应触发熔断
     *
     * <p>窗口满时按失败率阈值判断；窗口未满但连续全部失败时快速熔断。</p>
     */
    private boolean shouldTripOpen() {
        int size = slidingWindow.size();
        if (size == 0) return false;
        int failures = failureCount.get();
        if (size >= slidingWindowSize) {
            return (double) failures / size >= failureRateThreshold;
        }
        return failures >= slidingWindowSize;
    }

    private void tripOpen() {
        state = CircuitBreakerState.OPEN;
        openSince = System.currentTimeMillis();
    }
}