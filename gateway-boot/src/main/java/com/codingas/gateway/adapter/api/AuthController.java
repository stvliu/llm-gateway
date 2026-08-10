/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.application.auth.dto.ChangePasswordRequest;
import com.codingas.gateway.application.auth.dto.LoginRequest;
import com.codingas.gateway.application.auth.dto.LoginResponse;
import com.codingas.gateway.application.user.UserService;
import com.codingas.gateway.application.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供用户登录、登出、获取当前用户信息等认证相关的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public void logout() {
        userService.logout();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userService.getById(userId);
    }

    /**
     * 修改密码
     */
    @PatchMapping("/me/password")
    public void updatePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.changePassword(userId, request);
    }
}
