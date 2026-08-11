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

import com.codingas.gateway.application.user.UserService;
import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器
 *
 * <p>提供用户 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserApiKeyService userApiKeyService;

    /**
     * 创建用户
     */
    @PostMapping
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    /**
     * 查询用户列表
     */
    @GetMapping
    public PageResponse<UserResponse> query(@ModelAttribute UserQueryRequest request) {
        return userService.query(request);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    /**
     * 更新用户状态
     */
    @PatchMapping("/{id}/state")
    public UserResponse updateState(
            @PathVariable Long id,
            @Valid @RequestBody UserStateUpdateRequest request) {
        return userService.updateState(id, request);
    }

    /**
     * 分配用户角色
     */
    @PutMapping("/{id}/roles")
    public UserResponse assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleAssignRequest request) {
        return userService.assignRoles(id, request);
    }

    /**
     * 重置用户密码
     *
     * @param id 用户 ID
     * @return 含一次性明文密码的响应
     */
    @PostMapping("/{id}/reset-password")
    public ResetPasswordResponse resetPassword(@PathVariable Long id) {
        return userService.resetPassword(id);
    }

    // ==================== UserApiKey 子资源 ====================

    /**
     * 查询指定用户的所有 API Key
     */
    @GetMapping("/{userId}/api-keys")
    public List<UserApiKeyResponse> listUserApiKeys(@PathVariable Long userId) {
        return userApiKeyService.findByUserId(userId);
    }
}
