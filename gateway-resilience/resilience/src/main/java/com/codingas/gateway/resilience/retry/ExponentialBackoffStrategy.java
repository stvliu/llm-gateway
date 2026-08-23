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
package com.codingas.gateway.resilience.retry;

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
