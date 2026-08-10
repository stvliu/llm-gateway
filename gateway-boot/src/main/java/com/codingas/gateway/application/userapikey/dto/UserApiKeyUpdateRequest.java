/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.userapikey.dto;

/**
 * 更新用户 API Key 请求
 *
 * @param applicationId 应用 ID（可选，非 null 时表示补绑/转移）
 * @param name          密钥名称（可选）
 */
public record UserApiKeyUpdateRequest(
        Long applicationId,
        String name
) {
}
