package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 摘要响应
 */
public record UserApiKeySummaryResponse(
        Long id,
        Long userId,
        List<Long> productIds,
        String keyPrefix,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
