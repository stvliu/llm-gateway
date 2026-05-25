package com.codingas.gateway.infrastructure.resilience;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器
 *
 * <p>基于滑动窗口统计失败率，支持 CLOSED→OPEN→HALF_OPEN 状态转换。</p>
 */
public class CircuitBreaker {

    private final double failureRateThreshold;
    private final int slidingWindowSize;
    private final long openDurationMs;
    private final int halfOpenMaxAttempts;

    private final ConcurrentLinkedDeque<Boolean> slidingWindow = new ConcurrentLinkedDeque<>();
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
                if (System.currentTimeMillis() - openSince >= openDurationMs) {
                    state = CircuitBreakerState.HALF_OPEN;
                    halfOpenAttempts.set(0);
                    yield true;
                }
                yield false;
            }
            case HALF_OPEN -> halfOpenAttempts.incrementAndGet() <= halfOpenMaxAttempts;
        };
    }

    /**
     * 记录成功
     */
    public void recordSuccess() {
        record(true);
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.CLOSED;
            slidingWindow.clear();
        }
    }

    /**
     * 记录失败
     */
    public void recordFailure() {
        record(false);
        if (state == CircuitBreakerState.HALF_OPEN) {
            tripOpen();
        } else if (getFailureRate() >= failureRateThreshold) {
            tripOpen();
        }
    }

    public CircuitBreakerState getState() { return state; }

    private void record(boolean success) {
        slidingWindow.addLast(success);
        while (slidingWindow.size() > slidingWindowSize) {
            slidingWindow.pollFirst();
        }
    }

    private double getFailureRate() {
        if (slidingWindow.isEmpty()) return 0.0;
        long failures = slidingWindow.stream().filter(b -> !b).count();
        return (double) failures / slidingWindow.size();
    }

    private void tripOpen() {
        state = CircuitBreakerState.OPEN;
        openSince = System.currentTimeMillis();
    }
}