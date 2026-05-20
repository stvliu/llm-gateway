package com.codingas.gateway.infrastructure.actuator;

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
 * <p>至少一个 Provider UP → 整体 UP；全部 DOWN/UNKNOWN → 整体 DOWN。</p>
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

        boolean anyUp = false;
        for (var state : allStatuses) {
            builder.withDetail(state.providerCode(), Map.of(
                    "status", state.status().getCode(),
                    "consecutiveFailures", state.consecutiveFailures(),
                    "consecutiveSuccesses", state.consecutiveSuccesses(),
                    "lastError", state.lastError() != null ? state.lastError() : ""
            ));
            if (state.status() == Status.UP) {
                anyUp = true;
            }
        }

        if (anyUp) {
            builder.up();
        } else {
            builder.down();
        }
    }
}
