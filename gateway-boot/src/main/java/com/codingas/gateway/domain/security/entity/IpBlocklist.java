package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * IP 黑名单实体
 *
 * <p>记录被封锁的 IP 地址，支持临时封锁和永久封锁。</p>
 * <p>临时封锁在 expiresAt 时间后自动失效。</p>
 */
@Entity
@Table(name = "ip_blocklist")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IpBlocklist extends BaseEntity {

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "block_reason")
    private String blockReason;

    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "blocked_by")
    private Long blockedBy;
}