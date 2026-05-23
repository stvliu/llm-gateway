package com.codingas.gateway.application.userapikey.dto;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;

import java.util.List;

/**
 * 更新用户 API Key 请求
 */
public record UserApiKeyUpdateRequest(
        String name,
        List<Long> productIds,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state
) {
}