package com.codingas.gateway.application.userapikey.dto;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;

import java.util.List;

/**
 * 用户 API Key 更新请求
 *
 * @param name 密钥名称
 * @param models 可访问的模型列表
 * @param quotaLimit 额度限制
 * @param state 状态
 */
public record UserApiKeyUpdateRequest(
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state
) {
}