package com.codingas.gateway.application.userapikey.dto;

import java.time.Instant;

/**
 * 用户 API Key 响应
 *
 * @param id            主键
 * @param userId        用户 ID
 * @param applicationId 应用 ID（权限锚点）
 * @param keyPrefix     Key 前缀
 * @param keyPlain      明文 Key（仅创建/详情返回）
 * @param name          密钥名称
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
public record UserApiKeyResponse(
        Long id,
        Long userId,
        Long applicationId,
        String keyPrefix,
        String keyPlain,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
