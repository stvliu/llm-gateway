package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * IP 黑名单实体
 *
 * <p>记录被封锁的 IP 地址，支持临时封锁和永久封锁。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class IpBlocklist extends BaseEntity {

    private String ipAddress;

    private String blockReason;

    private Instant blockedAt;

    private Instant expiresAt;

    private Long blockedBy;

    /**
     * 检查封锁是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
