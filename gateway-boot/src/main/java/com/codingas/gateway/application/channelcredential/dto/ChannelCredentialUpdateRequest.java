package com.codingas.gateway.application.channelcredential.dto;

import com.codingas.gateway.domain.supply.enums.CredentialState;

/**
 * 渠道凭证更新请求
 *
 * @param priority 优先级
 * @param weight 权重
 * @param state 状态
 * @param description 描述
 */
public record ChannelCredentialUpdateRequest(
        Integer priority,
        Integer weight,
        CredentialState state,
        String description
) {
}
