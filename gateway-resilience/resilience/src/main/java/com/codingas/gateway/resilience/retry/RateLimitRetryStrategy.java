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
