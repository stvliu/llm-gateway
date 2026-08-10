/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.user.dto;

import com.codingas.gateway.domain.iam.enums.UserState;
import lombok.Data;
import java.time.Instant;

/**
 * 用户响应
 */
@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private UserState state;
    private Boolean emailVerified;
    /**
     * 用户角色：ADMIN（管理员）/ USER（普通用户）
     */
    private String role;
    /**
     * 是否为系统内建用户
     */
    private Boolean builtin;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
