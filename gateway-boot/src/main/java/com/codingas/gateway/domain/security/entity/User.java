package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.common.enums.UserStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;

/**
 * 用户实体
 *
 * <p>表示系统中的用户账户，包含认证和授权信息。</p>
 * <p>简化角色模型：通过 role 字段区分 ADMIN/USER 双角色。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class User extends BaseEntity {

    private String userCode;

    private String username;

    private String email;

    private String passwordHash;

    private String phone;

    private String avatarUrl;

    private UserStatus status = UserStatus.ACTIVE;

    /**
     * 用户角色：ADMIN（管理员）/ USER（普通用户）
     */
    private String role = "USER";

    private Boolean emailVerified = false;

    private Map<String, String> oauthProviders;

    private String piiSalt;

    private Instant lastLoginAt;

    private Instant deletedAt;

    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }

    /**
     * 检查邮箱是否已验证
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
