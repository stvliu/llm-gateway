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
