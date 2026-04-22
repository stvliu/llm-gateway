package com.codingas.gateway.core.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    /**
     * 用户编码 (业务标识)
     */
    @Column(name = "user_code", nullable = false, unique = true, length = 64)
    private String userCode;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 64)
    private String username;

    /**
     * 邮箱
     */
    @Column(name = "email", nullable = false, unique = true, length = 128)
    private String email;

    /**
     * 密码哈希 (BCrypt, cost≥12)
     */
    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 32)
    private String phone;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 邮箱已验证
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * OAuth 提供者列表 (JSON)
     */
    @Column(name = "oauth_providers", columnDefinition = "JSON")
    private String oauthProviders;

    /**
     * PII 脱敏盐值 (GDPR 删除权用)
     */
    @Column(name = "pii_salt", length = 64)
    private String piiSalt;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        /** 活跃 */
        ACTIVE,
        /** 禁用 */
        DISABLED,
        /** 锁定 */
        LOCKED,
        /** 已删除 */
        DELETED
    }
}
