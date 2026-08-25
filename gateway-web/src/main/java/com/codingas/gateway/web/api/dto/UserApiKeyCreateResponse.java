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
 * 用户 API Key 创建响应 DTO（HTTP 契约，包含仅展示一次的明文 Key）
 *
 * @param id          主键
 * @param keyPrefix   Key 前缀
 * @param apiKeyPlain 明文 API Key（仅创建时返回，后续不可获取）
 */
public record UserApiKeyCreateResponse(
        Long id,
        String keyPrefix,
        String apiKeyPlain
) {
    /**
     * 从创建后的 Key 实体转换（携带仅此一次可见的明文）
     *
     * @param apiKey 创建后的 Key 实体（含明文 keyPlain）
     * @return 创建响应 DTO
     */
    public static UserApiKeyCreateResponse from(UserApiKey apiKey) {
        return new UserApiKeyCreateResponse(
                apiKey.getId(),
                apiKey.getKeyPrefix(),
                apiKey.getKeyPlain());
    }
}
