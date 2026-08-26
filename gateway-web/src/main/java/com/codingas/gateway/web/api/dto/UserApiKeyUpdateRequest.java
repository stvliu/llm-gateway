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

import com.codingas.gateway.iam.apikey.UserApiKey;

/**
 * 更新用户 API Key 请求 DTO（HTTP 契约）
 *
 * @param applicationId 应用 ID（可选，非 null 时表示补绑/转移）
 * @param name          密钥名称（可选）
 */
public record UserApiKeyUpdateRequest(
        Long applicationId,
        String name
) {
    /**
     * 转换为 Key 实体（null 字段表示不更新）
     *
     * @return Key 实体
     */
    public UserApiKey toEntity() {
        UserApiKey apiKey = new UserApiKey();
        apiKey.setApplicationId(applicationId);
        apiKey.setName(name);
        return apiKey;
    }
}
