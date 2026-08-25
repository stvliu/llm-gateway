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

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserState;
import lombok.Data;

import java.time.Instant;

/**
 * 用户响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(User)} 从 {@code User} 实体裁剪敏感字段（passwordHash/piiSalt 等）后生成。</p>
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
    private String role;
    private Boolean builtin;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从用户实体转换（裁剪敏感字段）
     *
     * @param user 用户实体
     * @return 用户响应 DTO
     */
    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setState(user.getState());
        response.setEmailVerified(user.getEmailVerified());
        response.setRole(user.getRole());
        response.setBuiltin(user.getBuiltin());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * 从用户实体分页转换
     *
     * @param page 用户实体分页
     * @return 用户响应 DTO 分页
     */
    public static PageResponse<UserResponse> fromPage(PageResponse<User> page) {
        return PageResponse.of(
                page.getItems().stream().map(UserResponse::from).toList(),
                page.getPagination().getPage(),
                page.getPagination().getLimit(),
                page.getPagination().getTotal());
    }
}
