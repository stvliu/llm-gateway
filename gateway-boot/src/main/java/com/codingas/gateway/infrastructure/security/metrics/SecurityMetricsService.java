package com.codingas.gateway.infrastructure.security.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 安全指标服务
 *
 * <p>提供安全指标的统一访问接口。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityMetricsService {

    private final SecurityMetrics metrics;

    /**
     * 记录认证结果
     */
    public void recordAuth(boolean success, long latencyMs) {
        metrics.recordAuthLatency(latencyMs);

        if (success) {
            metrics.recordAuthSuccess();
        } else {
            metrics.recordAuthFailure();
        }
    }

    /**
     * 记录限流结果
     */
    public void recordRateLimit(boolean allowed) {
        if (allowed) {
            metrics.recordRateLimitAllowed();
        } else {
            metrics.recordRateLimitExceeded();
        }
    }

    /**
     * 记录暴力破解封禁
     */
    public void recordBruteForceBlock() {
        metrics.recordBruteForceBlock();
    }

    /**
     * 记录 IP 封禁
     */
    public void recordIpBlocked() {
        metrics.recordIpBlocked();
    }

    /**
     * 记录脱敏处理
     */
    public void recordMasking() {
        metrics.recordMaskingApplied();
    }

    /**
     * 获取认证成功率
     */
    public double getAuthSuccessRate() {
        return metrics.getAuthSuccessRate();
    }

    /**
     * 获取安全指标快照
     */
    public SecurityMetrics.MetricsSnapshot getSnapshot() {
        return metrics.getSnapshot();
    }
}
