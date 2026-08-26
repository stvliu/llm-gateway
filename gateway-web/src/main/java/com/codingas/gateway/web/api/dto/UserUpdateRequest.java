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

import com.codingas.gateway.iam.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户请求 DTO（HTTP 契约）
 */
@Data
public class UserUpdateRequest {
    @Size(min = 2, max = 64, message = "用户名长度必须在 2-64 之间")
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String avatarUrl;

    /**
     * 转换为用户实体（null 字段表示不更新）
     *
     * @return 用户实体
     */
    public User toEntity() {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(avatarUrl);
        return user;
    }
}
