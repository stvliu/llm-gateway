package com.codingas.gateway.security.ipblock;

import com.codingas.gateway.core.domain.entity.IpBlocklist;
import com.codingas.gateway.core.repository.IpBlocklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * IP 黑名单服务
 *
 * <p>提供动态 IP 封禁/解封、黑名单查询功能。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlocklistService {

    private final IpBlocklistRepository blocklistRepository;

    /**
     * 检查 IP 是否在黑名单中
     */
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        // 检查精确匹配
        Optional<IpBlocklist> exactMatch = blocklistRepository.findByIpAddressAndExpiresAtAfter(ipAddress, Instant.now());
        if (exactMatch.isPresent()) {
            return true;
        }

        // 检查 IP 范围
        return blocklistRepository.existsActiveBlockForIp(ipAddress, Instant.now());
    }

    /**
     * 封禁 IP
     *
     * @param ipAddress  IP 地址
     * @param blockType  封禁类型 (IP, CIDR, RANGE)
     * @param reason     封禁原因
     * @param blockedBy  封禁操作人 ID
     * @param durationMinutes 封禁时长（分钟），null 表示永久
     */
    @Transactional
    public IpBlocklist blockIp(String ipAddress, String blockType, String reason, Long blockedBy, Long durationMinutes) {
        IpBlocklist block = new IpBlocklist();
        block.setIpAddress(ipAddress);
        block.setBlockType(blockType != null ? blockType : "IP");
        block.setReason(reason);
        block.setBlockedBy(blockedBy);
        block.setBlockedAt(Instant.now());

        if (durationMinutes != null) {
            block.setExpiresAt(Instant.now().plusSeconds(durationMinutes * 60));
        }

        IpBlocklist saved = blocklistRepository.save(block);
        log.info("IP blocked: address={}, type={}, reason={}, by={}, expires={}",
            ipAddress, blockType, reason, blockedBy, block.getExpiresAt());

        return saved;
    }

    /**
     * 解封 IP
     */
    @Transactional
    public void unblockIp(Long id, Long unblockedBy) {
        IpBlocklist block = blocklistRepository.findById(id).orElse(null);
        if (block == null) {
            log.warn("Attempted to unblock non-existent block: id={}", id);
            return;
        }

        block.setUnblockedBy(unblockedBy);
        block.setUnblockedAt(Instant.now());
        blocklistRepository.save(block);

        log.info("IP unblocked: id={}, by={}", id, unblockedBy);
    }

    /**
     * 解封 IP by 地址
     */
    @Transactional
    public void unblockIp(String ipAddress, Long unblockedBy) {
        IpBlocklist block = blocklistRepository.findByIpAddress(ipAddress).orElse(null);
        if (block != null) {
            block.setUnblockedBy(unblockedBy);
            block.setUnblockedAt(Instant.now());
            blocklistRepository.save(block);
            log.info("IP unblocked: address={}, by={}", ipAddress, unblockedBy);
        }
    }

    /**
     * 清理过期封禁记录
     */
    @Transactional
    public void cleanupExpiredBlocks() {
        long deleted = blocklistRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired IP blocks", deleted);
        }
    }
}