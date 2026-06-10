package com.codingas.gateway.infrastructure.resilience;

/**
 * 指数退避策略
 *
 * <p>默认重试策略，退避时间按指数增长：delay = initial * multiplier^(attempt-1)。</p>
 */
public class ExponentialBackoffStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long backoffInitial;
    private final double backoffMultiplier;

    public ExponentialBackoffStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getMaxAttempts();
        this.backoffInitial = properties.getBackoffInitial();
        this.backoffMultiplier = properties.getBackoffMultiplier();
    }

    @Override
    public long calculateDelay(int attempt) {
        return (long) (backoffInitial * Math.pow(backoffMultiplier, attempt - 1));
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }
}
