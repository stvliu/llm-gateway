/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.auth.dto;

/**
 * 登录响应 DTO
 */
public record LoginResponse(
    UserResponse user,
    String token
) {
    public record UserResponse(
        Long id,
        String username,
        String email,
        String role
    ) {}
}
