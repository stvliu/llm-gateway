package com.codingas.gateway.infrastructure.model.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Provider 调用凭证 DO
 *
 * <p>JPA 实体，对应数据库 provider_api_keys 表。</p>
 */
/**
 * @deprecated 旧架构 DO，由 ProductApiKeyDo 替代
 */
@Deprecated(since = "2.0", forRemoval = true)
@Entity
@Table(name = "provider_api_keys", indexes = {
    @Index(name = "idx_provider_id", columnList = "provider_id"),
    @Index(name = "idx_state", columnList = "state")
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
    @Column(name = "state", nullable = false, length = 32)
    private ProviderApiKeyState state = ProviderApiKeyState.ACTIVE;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rpm_limit")
    private Integer rpmLimit;

    @Column(name = "tpm_limit")
    private Long tpmLimit;
}
