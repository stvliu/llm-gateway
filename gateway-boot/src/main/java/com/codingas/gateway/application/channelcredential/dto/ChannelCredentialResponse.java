package com.codingas.gateway.application.channelcredential.dto;

import com.codingas.gateway.domain.supply.enums.CredentialState;

import java.time.Instant;

/**
 * 渠道凭证响应（不含明文）
 *
 * @param id 主键
 * @param channelId 渠道 ID
 * @param apiKeyPrefix Key 前缀
 * @param apiKeyPlain 明文 API Key（前端脱敏显示）
 * @param name 密钥名称
 * @param description 描述
 * @param weight 权重
 * @param priority 优先级
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelCredentialResponse(
        Long id,
        Long channelId,
        String apiKeyPrefix,
        String apiKeyPlain,
        String name,
        String description,
        Integer weight,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {
}
