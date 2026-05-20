package com.codingas.gateway.application.userapikey.dto;

import com.codingas.gateway.domain.team.enums.UserApiKeyState;

import java.time.Instant;
import java.util.List;

/**
 * 用户 API Key 响应
 *
 * @param id 主键
 * @param teamId 团队 ID
 * @param productId 产品 ID
 * @param keyPrefix Key 前缀
 * @param name 密钥名称
 * @param models 可访问的模型列表
 * @param quotaLimit 额度限制
 * @param state 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record UserApiKeyResponse(
        Long id,
        Long teamId,
        Long productId,
        String keyPrefix,
        String name,
        List<String> models,
        Long quotaLimit,
        UserApiKeyState state,
        Instant createdAt,
        Instant updatedAt
) {
}