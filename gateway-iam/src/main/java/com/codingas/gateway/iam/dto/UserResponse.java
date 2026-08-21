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
package com.codingas.gateway.iam.dto;

import com.codingas.gateway.iam.user.UserState;
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
