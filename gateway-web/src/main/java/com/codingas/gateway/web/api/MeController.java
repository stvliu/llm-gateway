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

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.iam.apikey.UserApiKeyService;
import com.codingas.gateway.web.api.dto.UserApiKeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 当前用户信息控制器
 *
 * <p>提供 /api/v1/me/* 路径下的端点，操作当前登录用户的资源。</p>
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserApiKeyService userApiKeyService;

    /**
     * 查询当前用户的所有 API Key
     */
    @GetMapping("/api-keys")
    public List<UserApiKeyResponse> listMyApiKeys() {
        Long userId = StpUtil.getLoginIdAsLong();
        return UserApiKeyResponse.from(userApiKeyService.findByUserId(userId));
    }
}
