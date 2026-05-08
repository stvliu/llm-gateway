package com.codingas.gateway.infrastructure.model.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Provider 调用凭证 DO
 *
 * <p>JPA 实体，对应数据库 provider_api_keys 表。</p>
 */
@Entity
@Table(name = "provider_api_keys", indexes = {
    @Index(name = "idx_provider_id", columnList = "provider_id"),
    @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderApiKeyDo extends BaseDo {

    @Column(name = "provider_id")
    private Long providerId;

    @Column(name = "key_name", length = 64)
    private String keyName;

    @Column(name = "api_key", nullable = false, length = 512)
    private String apiKey;

    @Column(name = "priority")
    private Integer priority = 100;

    @Column(name = "weight")
    private Integer weight = 100;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProviderApiKeyStatus status = ProviderApiKeyStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "disabled_reason", length = 32)
    private ProviderApiKeyDisabledReason disabledReason;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "rpm_limit")
    private Integer rpmLimit;

    @Column(name = "tpm_limit")
    private Long tpmLimit;

    /**
     * API Key 状态枚举
     */
    public enum ProviderApiKeyStatus {
        ACTIVE,
        DISABLED,
        EXPIRED,
        RATE_LIMITED,
        OVERQUOTA,
        ERROR,
        DELETED
    }

    /**
     * API Key 禁用原因枚举
     */
    public enum ProviderApiKeyDisabledReason {
        MANUAL,
        RATE_LIMIT,
        QUOTA_EXCEEDED,
        AUTH_FAILED,
        EXPIRED,
        PROVIDER_ERROR
    }
}
