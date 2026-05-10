package com.codingas.gateway.infrastructure.security.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import com.codingas.gateway.domain.security.enums.GatewayApiKeyState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

/**
 * 网关访问凭证 DO
 *
 * <p>JPA 实体，对应数据库 gateway_api_keys 表。</p>
 */
@Entity
@Table(name = "gateway_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GatewayApiKeyDo extends BaseDo {

    @Column(name = "key_hash", nullable = false, length = 256)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDo user;

    @Column(name = "name", length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private GatewayApiKeyState state = GatewayApiKeyState.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ip_whitelist", columnDefinition = "json")
    private List<String> ipWhitelist;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
