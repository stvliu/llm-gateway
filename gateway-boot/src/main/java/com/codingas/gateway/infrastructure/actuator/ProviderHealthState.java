package com.codingas.gateway.infrastructure.actuator;

import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.time.Instant;

/**
 * Provider 健康状态
 *
 * <p>不可变 record，每次状态变更返回新实例。</p>
 */
public record ProviderHealthState(
        String providerCode,
        Status status,
        Instant lastCheckTime,
        Instant lastRequestTime,
        int consecutiveFailures,
        int consecutiveSuccesses,
        String lastError
) {
    /**
     * 创建初始状态（UNKNOWN）
     */
    public static ProviderHealthState initial(String providerCode) {
        return new ProviderHealthState(providerCode, Status.UNKNOWN, null, null, 0, 0, null);
    }

    /**
     * 记录请求成功
     *
     * <p>重置连续失败计数，累加连续成功计数。</p>
     */
    public ProviderHealthState withSuccess() {
        return new ProviderHealthState(providerCode, Status.UP, lastCheckTime, Instant.now(),
                0, consecutiveSuccesses + 1, null);
    }

    /**
     * 记录请求失败
     *
     * <p>重置连续成功计数，累加连续失败计数。状态保持不变，由 Tracker 根据阈值判断。</p>
     */
    public ProviderHealthState withFailure(String error) {
        return new ProviderHealthState(providerCode, status, lastCheckTime, Instant.now(),
                consecutiveFailures + 1, 0, error);
    }

    /**
     * 更新主动探测结果
     */
    public ProviderHealthState withProbe(Status probeStatus) {
        return new ProviderHealthState(providerCode, probeStatus, Instant.now(), lastRequestTime,
                probeStatus == Status.DOWN ? consecutiveFailures + 1 : 0,
                probeStatus == Status.UP ? consecutiveSuccesses + 1 : 0,
                probeStatus == Status.DOWN ? "probe failed" : null);
    }

    /**
     * 判断状态是否过期
     */
    public boolean isStale(Duration threshold) {
        if (lastCheckTime == null) {
            return true;
        }
        return Instant.now().isAfter(lastCheckTime.plus(threshold));
    }
}
