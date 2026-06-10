package com.codingas.gateway.infrastructure.resilience;

/**
 * 服务不可用重试策略
 *
 * <p>适用于 503 上游错误：固定 5s 退避，最多重试 3 次。</p>
 */
public class ServiceUnavailableStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long backoffFixed;

    public ServiceUnavailableStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getServiceUnavailable().getMaxAttempts();
        this.backoffFixed = properties.getServiceUnavailable().getBackoffFixed();
    }

    @Override
    public long calculateDelay(int attempt) {
        return backoffFixed;
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }
}
