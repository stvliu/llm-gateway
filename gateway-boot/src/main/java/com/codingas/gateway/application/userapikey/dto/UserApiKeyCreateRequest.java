/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.userapikey.dto;

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
