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
package com.codingas.gateway.provider.health;

import com.codingas.gateway.provider.health.ProviderHealthTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Provider 注册表健康指标
 *
 * <p>聚合所有 LLM Provider 的健康状态。</p>
 * <p>至少一个 Provider 明确 DOWN → 整体 DOWN；全部 UP/UNKNOWN → 整体 UP（无流量≠不健康）。</p>
 */
@Component
@RequiredArgsConstructor
public class ProviderRegistryHealthIndicator extends AbstractHealthIndicator {

    private final ProviderHealthTracker healthTracker;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        var allStatuses = healthTracker.getAllStatuses();

        if (allStatuses.isEmpty()) {
            builder.withDetail("message", "无已注册的 Provider").down();
            return;
        }

        boolean anyDown = false;
        for (var state : allStatuses) {
            builder.withDetail(state.providerCode(), Map.of(
                    "status", state.status().getCode(),
                    "consecutiveFailures", state.consecutiveFailures(),
                    "consecutiveSuccesses", state.consecutiveSuccesses(),
                    "lastError", state.lastErrorMessage() != null ? state.lastErrorMessage() : ""
            ));
            // 只有明确 DOWN 才视为不健康；UNKNOWN（初始态/无流量）视为健康
            if (state.status() == Status.DOWN) {
                anyDown = true;
            }
        }

        if (anyDown) {
            builder.down();
        } else {
            builder.up();
        }
    }
}
