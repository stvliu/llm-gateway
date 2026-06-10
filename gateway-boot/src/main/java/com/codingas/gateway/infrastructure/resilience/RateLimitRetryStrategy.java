package com.codingas.gateway.infrastructure.resilience;

import java.util.Random;

/**
 * 限流重试策略
 *
 * <p>适用于 429 限流错误：初始 2s，倍率 2x，最大 60s，加抖动防止惊群效应。</p>
 */
public class RateLimitRetryStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long backoffInitial;
    private final double backoffMultiplier;
    private final long maxBackoff;
    private final double jitterRate;
    private final Random random = new Random();

    public RateLimitRetryStrategy(GatewayRetryProperties properties) {
        GatewayRetryProperties.RateLimitConfig cfg = properties.getRateLimit();
        this.maxAttempts = cfg.getMaxAttempts();
        this.backoffInitial = cfg.getBackoffInitial();
        this.backoffMultiplier = cfg.getBackoffMultiplier();
        this.maxBackoff = cfg.getMaxBackoff();
        this.jitterRate = cfg.getJitterRate();
    }

    @Override
    public long calculateDelay(int attempt) {
        long delay = (long) (backoffInitial * Math.pow(backoffMultiplier, attempt - 1));
        delay = Math.min(delay, maxBackoff);
        double jitter = 1.0 + (random.nextDouble() - 0.5) * 2 * jitterRate;
        return (long) (delay * jitter);
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }
}
