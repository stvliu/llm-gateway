package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Provider 调用凭证
 *
 * <p>网关调用大模型 Provider 时的凭据，属于系统维度。</p>
 * <p>一个 Provider 可有多个 Key（主备/轮换），管理员配置。</p>
 */
@Entity
@Table(name = "provider_api_keys", indexes = {
    @Index(name = "idx_provider_id", columnList = "provider_id")
})
@Getter
@Setter
public class ProviderApiKey extends BaseEntity {

    /**
     * 密钥编码 (业务标识)
     */
    @Column(name = "key_code", nullable = false, unique = true, length = 64)
    private String keyCode;

    /**
     * 所属 Provider ID
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /**
     * Key 名称（如"主Key"、"备用Key"）
     */
    @Column(name = "key_name", length = 64)
    private String keyName;

    /**
     * API Key（加密存储）
     */
    @Column(name = "api_key", nullable = false, length = 512)
    private String apiKey;

    /**
     * 加密后的 API Key
     */
    @Column(name = "encrypted_api_key", length = 512)
    private String encryptedApiKey;

    /**
     * 优先级 (数值越大越优先)
     */
    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 密钥状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProviderApiKeyStatus status = ProviderApiKeyStatus.ACTIVE;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /**
     * 过期时间（NULL 表示永不过期）
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Provider 调用凭证状态枚举
     */
    public enum ProviderApiKeyStatus {
        /** 活跃 */
        ACTIVE,
        /** 已禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }
}
