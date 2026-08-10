/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channelcredential.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 渠道凭证创建请求
 *
 * <p>注意：channelId 由适配层（Controller/gRPC stub）从协议上下文中提取并填充，
 * 请求体本身不包含此字段，因此此处不做 @NotNull 校验。</p>
 *
 * @param channelId 渠道 ID（适配层填充）
 * @param apiKey API Key 明文（创建后加密存储）
 * @param priority 优先级（数值越小优先级越高）
 * @param weight 权重（同优先级下按权重分配流量）
 * @param description 描述
 */
public record ChannelCredentialCreateRequest(
        Long channelId,
        @NotBlank(message = "API Key 不能为空")
        String apiKey,
        Integer priority,
        Integer weight,
        String description
) {
}
