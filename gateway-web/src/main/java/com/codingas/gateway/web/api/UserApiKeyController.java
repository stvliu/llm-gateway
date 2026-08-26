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
package com.codingas.gateway.web.api;

import com.codingas.gateway.iam.apikey.UserApiKeyManager;
import com.codingas.gateway.web.api.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 API Key 管理 API
 */
@RestController
@RequestMapping("/api/v1/user-api-keys")
public class UserApiKeyController {

    private final UserApiKeyManager userApiKeyManager;

    public UserApiKeyController(UserApiKeyManager userApiKeyManager) {
        this.userApiKeyManager = userApiKeyManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserApiKeyCreateResponse create(@Valid @RequestBody UserApiKeyCreateRequest request) {
        return UserApiKeyCreateResponse.from(userApiKeyManager.create(request.toEntity()));
    }

    @GetMapping(params = "userId")
    public List<UserApiKeyResponse> findByUserId(@RequestParam Long userId) {
        return UserApiKeyResponse.from(userApiKeyManager.findByUserId(userId));
    }

    /** 查询所有 API Key（管理员用） */
    @GetMapping
    public List<UserApiKeyResponse> findAll() {
        return UserApiKeyResponse.from(userApiKeyManager.findAllNonDeleted());
    }

    @GetMapping("/{id}")
    public UserApiKeyResponse getById(@PathVariable Long id) {
        return UserApiKeyResponse.from(userApiKeyManager.getById(id));
    }

    @GetMapping("/{id}/detail")
    public UserApiKeyResponse getDetailById(@PathVariable Long id) {
        return UserApiKeyResponse.from(userApiKeyManager.getDetailById(id));
    }

    @PutMapping("/{id}")
    public UserApiKeyResponse update(@PathVariable Long id, @Valid @RequestBody UserApiKeyUpdateRequest request) {
        return UserApiKeyResponse.from(userApiKeyManager.update(id, request.toEntity()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userApiKeyManager.delete(id);
    }
}
