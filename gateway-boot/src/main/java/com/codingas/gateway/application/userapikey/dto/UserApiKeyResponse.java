package com.codingas.gateway.application.userapikey.dto;

import com.codingas.gateway.domain.iam.enums.UserApiKeyState;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 响应
 */
public record UserApiKeyResponse(
        Long id,
        Long userId,
        List<Long> productIds,
        List<ChannelBrief> channels,
        String keyPrefix,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {
}