package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * API 密钥实体
 *
 * <p>用户生成的调用凭证，格式为 sk-xxxxxxxxxxxxxxxx</p>
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
public class ApiKey extends BaseEntity {

    /**
     * 密钥编码 (业务标识)
     */
    @Column(name = "key_code", nullable = false, unique = true, length = 128)
    private String keyCode;

    /**
     * Key 哈希 (用于验证)
     */
    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;

    /**
     * 用户 ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 团队 ID
     */
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    /**
     * 密钥状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "key_status", nullable = false, length = 32)
    private KeyStatus keyStatus = KeyStatus.ACTIVE;

    /**
     * 额度上限
     */
    @Column(name = "quota", nullable = false, precision = 20, scale = 6)
    private BigDecimal quota = BigDecimal.ZERO;

    /**
     * 已用额度
     */
    @Column(name = "used_quota", nullable = false, precision = 20, scale = 6)
    private BigDecimal usedQuota = BigDecimal.ZERO;

    /**
     * 过期时间 (NULL 表示永不过期)
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 模型白名单 (JSON 数组)
     */
    @Column(name = "model_whitelist", columnDefinition = "JSON")
    private String modelWhitelist;

    /**
     * IP 白名单 (JSON 数组，支持 CIDR)
     */
    @Column(name = "ip_whitelist", columnDefinition = "JSON")
    private String ipWhitelist;

    /**
     * 密钥状态枚举
     */
    public enum KeyStatus {
        /** 活跃 */
        ACTIVE,
        /** 暂停 */
        SUSPENDED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }
}
