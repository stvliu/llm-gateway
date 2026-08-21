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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建用户 API Key 请求
 *
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点，创建时必填）
 * @param name          密钥名称
 */
public record UserApiKeyCreateRequest(
        @NotNull(message = "用户 ID 不能为空")
        Long userId,
        @NotNull(message = "应用 ID 不能为空")
        Long applicationId,
        @NotBlank(message = "密钥名称不能为空")
        String name
) {
}
