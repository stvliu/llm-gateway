package com.codingas.gateway.application.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 */
public record LoginRequest(
    @NotBlank(message = "用户名不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password,

    boolean rememberMe
) {}
