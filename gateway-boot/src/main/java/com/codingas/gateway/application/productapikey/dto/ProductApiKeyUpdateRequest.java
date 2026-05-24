package com.codingas.gateway.application.productapikey.dto;

import com.codingas.gateway.domain.supply.enums.CredentialState;

/**
 * 产品 API Key 更新请求
 *
 * @param priority 优先级
 * @param weight 权重
 * @param state 状态
 * @param description 描述
 */
public record ProductApiKeyUpdateRequest(
        Integer priority,
        Integer weight,
        CredentialState state,
        String description
) {
}
