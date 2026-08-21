/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.dto.ChangePasswordRequest;
import com.codingas.gateway.iam.dto.LoginRequest;
import com.codingas.gateway.iam.dto.LoginResponse;
import com.codingas.gateway.iam.service.UserService;
import com.codingas.gateway.iam.dto.UserResponse;
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
