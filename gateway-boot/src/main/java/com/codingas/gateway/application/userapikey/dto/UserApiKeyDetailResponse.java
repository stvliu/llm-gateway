package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 详情响应（含明文 Key，仅创建时和详情页返回）
 */
public record UserApiKeyDetailResponse(
        Long id,
        Long userId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
