package com.codingas.gateway.application.channelcredential.dto;

import com.codingas.gateway.domain.supply.enums.CredentialState;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道凭证更新请求
 *
 * <p>注意：channelId 和 id 由适配层（Controller/gRPC stub）从协议上下文中提取并填充，
 * 请求体本身不包含这些字段。</p>
 *
 * @param channelId 渠道 ID（适配层填充）
 * @param id 凭证 ID（适配层填充）
 * @param priority 优先级
 * @param weight 权重
 * @param description 描述
 * @param apiKey 可选，传值则替换 API Key
 */
public record ChannelCredentialUpdateRequest(
        Long channelId,
        Long id,
        Integer priority,
        Integer weight,
        String description,
        String apiKey
) {
}
