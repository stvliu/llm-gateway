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
    @Index(name = "idx_provider_id", columnList = "provider_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProviderApiKeyDo extends BaseDo {

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "key_name", length = 64)
    private String keyName;

    @Column(name = "api_key", nullable = false, length = 512)
    private String apiKey;

    @Column(name = "encrypted_api_key", length = 512)
    private String encryptedApiKey;

    @Column(name = "priority")
    private Integer priority = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProviderApiKeyStatus status = ProviderApiKeyStatus.ACTIVE;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public enum ProviderApiKeyStatus {
        ACTIVE,
        DISABLED,
        EXPIRED,
        DELETED
    }
}
