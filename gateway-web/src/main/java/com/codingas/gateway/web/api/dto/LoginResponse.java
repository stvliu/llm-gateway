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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.iam.user.LoginResult;
import com.codingas.gateway.iam.user.User;

import java.util.List;

/**
 * 登录响应 DTO（HTTP 契约）
 */
public record LoginResponse(
    UserResponse user,
    String token
) {
    /**
     * 登录响应中的用户信息（含角色推导的权限码，前端 UI 直接消费）
     */
    public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        List<String> permissions
    ) {}

    /**
     * 从登录用例结果转换
     *
     * @param result 登录用例结果
     * @return 登录响应 DTO
     */
    public static LoginResponse from(LoginResult result) {
        User user = result.user();
        return new LoginResponse(
                new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        result.permissions()),
                result.token());
    }
}
