package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.gateway.IpBlockGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 暴力破解防护服务
 *
 * <p>检测并阻止暴力破解认证尝试。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BruteForceProtectionService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 15;

    private final IpBlockGateway ipBlockGateway;
    private final ConcurrentHashMap<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    /**
     * 记录失败的认证尝试
     *
     * @param clientIp 客户端 IP
     */
    public void recordFailedAttempt(String clientIp) {
        int attempts = failedAttempts.computeIfAbsent(clientIp, k -> new AtomicInteger(0))
            .incrementAndGet();

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            log.warn("Blocking IP due to {} failed attempts: {}", attempts, clientIp);
            ipBlockGateway.block(clientIp, "Brute force protection",
                null, Instant.now().plusSeconds(BLOCK_DURATION_MINUTES * 60L));
            failedAttempts.remove(clientIp);
        }
    }

    /**
     * 清除失败的认证尝试记录
     *
     * @param clientIp 客户端 IP
     */
    public void clearFailedAttempts(String clientIp) {
        failedAttempts.remove(clientIp);
    }
}
