package com.codingas.gateway.infrastructure.metrics;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 安全指标定义
 *
 * <p>简单的内存指标收集，支持后续集成 Micrometer/Prometheus。</p>
 */
@Slf4j
@Component
public class SecurityMetrics {

    // 认证指标
    @Getter
    private final AtomicLong authSuccessCount = new AtomicLong(0);

    @Getter
    private final AtomicLong authFailureCount = new AtomicLong(0);

    private final AtomicLong authTotalLatencyMs = new AtomicLong(0);
    private final AtomicLong authRequestCount = new AtomicLong(0);

    // 限流指标
    @Getter
    private final AtomicLong rateLimitExceededCount = new AtomicLong(0);

    @Getter
    private final AtomicLong rateLimitAllowedCount = new AtomicLong(0);

    // 暴力破解防护指标
    @Getter
    private final AtomicLong bruteForceBlockCount = new AtomicLong(0);

    @Getter
    private final AtomicInteger currentBruteForceBlocked = new AtomicInteger(0);

    // 脱敏指标
    @Getter
    private final AtomicLong maskingAppliedCount = new AtomicLong(0);

    @Getter
    private final AtomicLong maskingHitCount = new AtomicLong(0);

    // IP 黑名单指标
    @Getter
    private final AtomicLong ipBlockedCount = new AtomicLong(0);

    @Getter
    private final AtomicInteger currentBlockedIps = new AtomicInteger(0);

    /**
     * 记录认证成功
     */
    public void recordAuthSuccess() {
        authSuccessCount.incrementAndGet();
    }

    /**
     * 记录认证失败
     */
    public void recordAuthFailure() {
        authFailureCount.incrementAndGet();
    }

    /**
     * 记录认证延迟
     */
    public void recordAuthLatency(long millis) {
        authTotalLatencyMs.addAndGet(millis);
        authRequestCount.incrementAndGet();
    }

    /**
     * 获取平均认证延迟
     */
    public double getAverageAuthLatency() {
        long count = authRequestCount.get();
        if (count == 0) return 0;
        return (double) authTotalLatencyMs.get() / count;
    }

    /**
     * 记录限流触发
     */
    public void recordRateLimitExceeded() {
        rateLimitExceededCount.incrementAndGet();
    }

    /**
     * 记录限流允许
     */
    public void recordRateLimitAllowed() {
        rateLimitAllowedCount.incrementAndGet();
    }

    /**
     * 记录暴力破解封禁
     */
    public void recordBruteForceBlock() {
        bruteForceBlockCount.incrementAndGet();
        currentBruteForceBlocked.incrementAndGet();
    }

    /**
     * 记录暴力破解封禁解除
     */
    public void recordBruteForceUnblock() {
        currentBruteForceBlocked.decrementAndGet();
    }

    /**
     * 记录脱敏处理
     */
    public void recordMaskingApplied() {
        maskingAppliedCount.incrementAndGet();
    }

    /**
     * 记录脱敏命中
     */
    public void recordMaskingHit() {
        maskingHitCount.incrementAndGet();
    }

    /**
     * 记录 IP 封禁
     */
    public void recordIpBlocked() {
        ipBlockedCount.incrementAndGet();
        currentBlockedIps.incrementAndGet();
    }

    /**
     * 记录 IP 解封
     */
    public void recordIpUnblocked() {
        currentBlockedIps.decrementAndGet();
    }

    /**
     * 获取认证成功率
     */
    public double getAuthSuccessRate() {
        long total = authSuccessCount.get() + authFailureCount.get();
        if (total == 0) return 1.0;
        return (double) authSuccessCount.get() / total;
    }

    /**
     * 获取指标快照
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            authSuccessCount.get(),
            authFailureCount.get(),
            getAuthSuccessRate(),
            getAverageAuthLatency(),
            rateLimitExceededCount.get(),
            rateLimitAllowedCount.get(),
            bruteForceBlockCount.get(),
            currentBruteForceBlocked.get(),
            maskingAppliedCount.get(),
            maskingHitCount.get(),
            ipBlockedCount.get(),
            currentBlockedIps.get()
        );
    }

    /**
     * 指标快照
     */
    public record MetricsSnapshot(
        long authSuccess,
        long authFailure,
        double authSuccessRate,
        double avgAuthLatencyMs,
        long rateLimitExceeded,
        long rateLimitAllowed,
        long bruteForceBlocks,
        int currentBruteForceBlocked,
        long maskingApplied,
        long maskingHits,
        long ipBlocked,
        int currentBlockedIps
    ) {}
}
