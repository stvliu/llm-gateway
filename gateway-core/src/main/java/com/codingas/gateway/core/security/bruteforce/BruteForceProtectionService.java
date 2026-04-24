package com.codingas.gateway.core.security.bruteforce;

import com.codingas.gateway.core.security.ipblock.IpBlocklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 暴力破解防护服务
 *
 * <p>检测连续认证失败，5 次失败后封禁 IP 15 分钟。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BruteForceProtectionService {

    private static final int MAX_FAILURES = 5;
    private static final int BAN_DURATION_MINUTES = 15;

    private final FailedAttemptTracker failedAttemptTracker;
    private final IpBlocklistService ipBlocklistService;

    /**
     * 记录认证失败
     */
    public void recordAuthFailure(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        long failures = failedAttemptTracker.increment(ipAddress);

        if (failures >= MAX_FAILURES) {
            // 达到阈值，封禁 IP
            log.warn("Brute force detected: ip={}, failures={}", ipAddress, failures);

            ipBlocklistService.blockIp(
                ipAddress,
                "IP",
                "Brute force protection: " + failures + " auth failures",
                0L, // system action
                (long) BAN_DURATION_MINUTES
            );

            // 清除失败计数
            failedAttemptTracker.delete(ipAddress);
        }
    }

    /**
     * 认证成功时清除失败记录
     */
    public void clearAuthFailures(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        failedAttemptTracker.delete(ipAddress);
    }

    /**
     * 获取当前失败次数
     */
    public int getFailureCount(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }

        return failedAttemptTracker.get(ipAddress);
    }

    /**
     * 检查 IP 是否被临时封禁（由于暴力破解）
     */
    public boolean isTempBlocked(String ipAddress) {
        return getFailureCount(ipAddress) >= MAX_FAILURES;
    }
}