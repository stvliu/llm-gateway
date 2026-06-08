package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 响应
 */
public record UserApiKeyResponse(
        Long id,
        Long userId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
