package com.codingas.gateway.infrastructure.security.database.dataobject;

import com.codingas.gateway.domain.security.enums.UserStatus;
import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 用户 DO
 *
 * <p>JPA 实体，对应数据库 users 表。</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDo extends BaseDo {

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 用户角色：ADMIN（管理员）/ USER（普通用户）
     */
    @Column(name = "role", length = 32)
    private String role = "USER";

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "oauth_providers", columnDefinition = "json")
    private Map<String, String> oauthProviders;

    @Column(name = "pii_salt", length = 64)
    private String piiSalt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
