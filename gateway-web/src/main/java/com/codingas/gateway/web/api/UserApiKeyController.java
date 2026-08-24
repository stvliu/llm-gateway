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

import com.codingas.gateway.iam.service.UserApiKeyService;
import com.codingas.gateway.iam.dto.*;
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

    private final UserApiKeyService userApiKeyService;

    public UserApiKeyController(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserApiKeyCreateResponse create(@Valid @RequestBody UserApiKeyCreateRequest request) {
        return userApiKeyService.create(request);
    }

    @GetMapping(params = "userId")
    public List<UserApiKeyResponse> findByUserId(@RequestParam Long userId) {
        return userApiKeyService.findByUserId(userId);
    }

    /** 查询所有 API Key（管理员用） */
    @GetMapping
    public List<UserApiKeyResponse> findAll() {
        return userApiKeyService.findAllNonDeleted();
    }

    @GetMapping("/{id}")
    public UserApiKeyResponse getById(@PathVariable Long id) {
        return userApiKeyService.getById(id);
    }

    @GetMapping("/{id}/detail")
    public UserApiKeyDetailResponse getDetailById(@PathVariable Long id) {
        return userApiKeyService.getDetailById(id);
    }

    @PutMapping("/{id}")
    public UserApiKeyResponse update(@PathVariable Long id, @Valid @RequestBody UserApiKeyUpdateRequest request) {
        return userApiKeyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userApiKeyService.delete(id);
    }
}
