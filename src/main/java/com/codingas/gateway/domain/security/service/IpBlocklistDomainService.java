package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.gateway.IpBlockGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * IP 黑名单服务
 *
 * <p>提供动态 IP 封禁/解封、黑名单查询功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlocklistDomainService {

    private final IpBlockGateway ipBlockGateway;

    /**
     * 检查 IP 是否在黑名单中
     */
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        return ipBlockGateway.isBlocked(ipAddress);
    }

    /**
     * 封禁 IP（永久）
     */
    public void blockIp(String ipAddress, String reason, Long blockedBy) {
        ipBlockGateway.block(ipAddress, reason, blockedBy, null);
        log.info("IP blocked: address={}, reason={}, by={}", ipAddress, reason, blockedBy);
    }

    /**
     * 封禁 IP（临时）
     */
    public void blockIp(String ipAddress, String reason, Long blockedBy, long durationMinutes) {
        java.time.Instant expiresAt = java.time.Instant.now().plusSeconds(durationMinutes * 60);
        ipBlockGateway.block(ipAddress, reason, blockedBy, expiresAt);
        log.info("IP blocked: address={}, reason={}, by={}, expires={}",
            ipAddress, reason, blockedBy, expiresAt);
    }

    /**
     * 解封 IP
     */
    public void unblockIp(String ipAddress) {
        ipBlockGateway.unblock(ipAddress);
        log.info("IP unblocked: address={}", ipAddress);
    }
}
