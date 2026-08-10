/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.iam.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import com.codingas.gateway.domain.iam.enums.UserState;
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

    private String username;

    private String email;

    private String passwordHash;

    private String phone;

    private String avatarUrl;

    private UserState state = UserState.ACTIVE;

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
     * 是否为系统内建用户（不可删除、不可降级、不可禁用）
     */
    private Boolean builtin = false;

    /**
     * 检查用户是否激活
     */
    public boolean isActive() {
        return UserState.ACTIVE.equals(state);
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * 检查是否为内建用户
     */
    public boolean isBuiltin() {
        return Boolean.TRUE.equals(builtin);
    }
}
