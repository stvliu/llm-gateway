package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.Instant;

/**
 * 网关访问凭证实体
 *
 * <p>存储用户的 API Key 信息，包括密钥哈希、状态和有效期。</p>
 * <p>实际密钥不在数据库中存储，仅存储其哈希值用于验证。</p>
 *
 * @see GatewayApiKeyStatus
 */
@Entity
@Table(name = "gateway_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class GatewayApiKey extends BaseEntity {

    @Column(name = "key_code", nullable = false, unique = true, length = 64)
    private String keyCode;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GatewayApiKeyStatus status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "rate_limit_config_id")
    private Long rateLimitConfigId;

    public enum GatewayApiKeyStatus {
        ACTIVE, INACTIVE, EXPIRED, REVOKED
    }
}