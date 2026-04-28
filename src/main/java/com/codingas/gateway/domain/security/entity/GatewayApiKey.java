package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * 网关访问凭证实体
 *
 * <p>用户调用 LLM-Gateway 网关的凭据，格式为 sk-xxxxxxxx。</p>
 */
@Entity
@Table(name = "gateway_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GatewayApiKey extends BaseEntity {

    @Column(name = "key_code", nullable = false, unique = true, length = 128)
    private String keyCode;

    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ip_whitelist", columnDefinition = "json")
    private List<String> ipWhitelist;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum ApiKeyStatus {
        /** 正常 */
        ACTIVE,
        /** 禁用 */
        DISABLED,
        /** 已过期 */
        EXPIRED,
        /** 已删除 */
        DELETED
    }
}