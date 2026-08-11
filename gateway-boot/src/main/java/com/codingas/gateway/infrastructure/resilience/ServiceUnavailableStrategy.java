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
