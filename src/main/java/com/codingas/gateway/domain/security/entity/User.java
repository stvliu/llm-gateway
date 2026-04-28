package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户实体
 *
 * <p>表示系统中的用户账户，包含认证和授权信息。</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User extends BaseEntity {

    @Column(name = "user_code", nullable = false, unique = true, length = 64)
    private String userCode;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRole> userRoles = new ArrayList<>();
}
