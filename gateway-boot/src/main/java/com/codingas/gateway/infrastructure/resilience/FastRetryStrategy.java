/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.resilience;

/**
 * 快速重试策略
 *
 * <p>适用于 504 超时错误：固定 500ms 退避，最多重试 2 次。</p>
 */
public class FastRetryStrategy implements RetryStrategy {

    private final int maxAttempts;
    private final long backoffFixed;

    public FastRetryStrategy(GatewayRetryProperties properties) {
        this.maxAttempts = properties.getFastRetry().getMaxAttempts();
        this.backoffFixed = properties.getFastRetry().getBackoffFixed();
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
