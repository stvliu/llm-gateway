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
package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 响应
 *
 * @param id            主键
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点）
 * @param keyPrefix     Key 前缀
 * @param keyPlain      明文 Key（仅创建/详情返回）
 * @param name          密钥名称
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
public record UserApiKeyResponse(
        Long id,
        Long userId,
        Long applicationId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
