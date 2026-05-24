package com.codingas.gateway.application.productapikey.dto;

import com.codingas.gateway.domain.supply.enums.CredentialState;

import java.time.Instant;

/**
 * 产品 API Key 响应（不含明文）
 *
 * @param id 主键
 * @param productId 产品 ID
 * @param apiKeyPrefix Key 前缀
 * @param name 密钥名称
 * @param description 描述
 * @param weight 权重
 * @param priority 优先级
 * @param state 状态
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ProductApiKeyResponse(
        Long id,
        Long channelId,
        String apiKeyPrefix,
        String name,
        String description,
        Integer weight,
        Integer priority,
        CredentialState state,
        Instant createdAt,
        Instant updatedAt
) {
}