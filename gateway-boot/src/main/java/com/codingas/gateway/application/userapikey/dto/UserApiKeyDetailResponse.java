package com.codingas.gateway.application.userapikey.dto;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 详情响应（含明文 Key，仅创建时和详情页返回）
 */
public record UserApiKeyDetailResponse(
        Long id,
        Long teamId,
        Long userId,
        List<Long> productIds,
        List<ProductBrief> products,
        String keyPrefix,
        String keyPlain,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {
}