package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * IP 黑名单实体
 *
 * <p>记录被禁止访问的 IP 地址、IP 段或 CIDR 范围。</p>
 *
 * <p>表名: ip_blocklist</p>
 *
 * @see BaseEntity
 */
@Entity
@Table(
    name = "ip_blocklist",
    indexes = {
        @Index(name = "idx_ip_blocklist_ip_address", columnList = "ip_address"),
        @Index(name = "idx_ip_blocklist_block_type", columnList = "block_type")
    }
)
@Getter
@Setter
public class IpBlocklist extends BaseEntity {

    /**
     * 具体 IP 地址 (当 block_type 为 "IP" 时)
     */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * IP 段起始地址 (当 block_type 为 "RANGE" 时)
     */
    @Column(name = "ip_range_start", length = 64)
    private String ipRangeStart;

    /**
     * IP 段结束地址 (当 block_type 为 "RANGE" 时)
     */
    @Column(name = "ip_range_end", length = 64)
    private String ipRangeEnd;

    /**
     * 封锁类型
     *
     * <p>"IP" - 单个 IP, "CIDR" - CIDR 网段, "RANGE" - IP 范围</p>
     */
    @Column(name = "block_type", nullable = false, length = 32)
    private String blockType;

    /**
     * 封锁原因
     */
    @Column(name = "reason", nullable = false, length = 256)
    private String reason;

    /**
     * 执行封锁的管理员用户 ID
     */
    @Column(name = "blocked_by", nullable = false)
    private Long blockedBy;

    /**
     * 封锁时间
     */
    @Column(name = "blocked_at", nullable = false)
    private Instant blockedAt;

    /**
     * 过期时间 (null 表示永久封锁)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * 解除封锁的管理员用户 ID (已解除时)
     */
    @Column(name = "unblocked_by")
    private Long unblockedBy;

    /**
     * 解除封锁时间 (已解除时)
     */
    @Column(name = "unblocked_at")
    private Instant unblockedAt;
}
