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
package com.codingas.gateway.infrastructure.actuator;

import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.time.Instant;

/**
 * Provider 健康状态记录
 *
 * <p>基于被动推断：连续失败 ≥ failureThreshold → DOWN；DOWN 后连续成功 ≥ successThreshold → UP。</p>
 *
 * @param providerCode         供应商编码
 * @param status               当前健康状态
 * @param lastRequestTime      最后一次请求时间
 * @param consecutiveFailures  连续失败次数
 * @param consecutiveSuccesses 连续成功次数
 * @param lastErrorMessage     最后一次错误信息
 */
public record ProviderHealthState(
        String providerCode,
        Status status,
        Instant lastRequestTime,
        int consecutiveFailures,
        int consecutiveSuccesses,
        String lastErrorMessage
) {

    /**
     * 创建初始状态（UNKNOWN）
     */
    public static ProviderHealthState initial(String providerCode) {
        return new ProviderHealthState(providerCode, Status.UNKNOWN, null, 0, 0, null);
    }

    /**
     * 记录请求成功
     */
    public ProviderHealthState withSuccess() {
        int newSuccesses = consecutiveSuccesses + 1;
        return new ProviderHealthState(providerCode, Status.UP, Instant.now(), 0, newSuccesses, null);
    }

    /**
     * 记录请求失败
     */
    public ProviderHealthState withFailure(String errorMessage) {
        int newFailures = consecutiveFailures + 1;
        return new ProviderHealthState(providerCode, Status.DOWN, Instant.now(), newFailures, 0, errorMessage);
    }

    /**
     * 判断状态是否过期
     */
    public boolean isStale(Duration threshold) {
        if (lastRequestTime == null) {
            return true;
        }
        return Instant.now().isAfter(lastRequestTime.plus(threshold));
    }
}