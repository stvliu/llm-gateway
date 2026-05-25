package com.codingas.gateway.application.channelcredential.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道凭证创建请求
 *
 * @param channelId 渠道 ID
 * @param apiKey API Key 明文（创建后加密存储）
 * @param priority 优先级（数值越小优先级越高）
 * @param weight 权重（同优先级下按权重分配流量）
 * @param description 描述
 */
public record ChannelCredentialCreateRequest(
        @NotNull(message = "渠道 ID 不能为空")
        Long channelId,
        @NotBlank(message = "API Key 不能为空")
        String apiKey,
        Integer priority,
        Integer weight,
        String description
) {
}
