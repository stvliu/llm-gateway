package com.codingas.gateway.application.productapikey.dto;

import com.codingas.gateway.domain.product.enums.ProductApiKeyState;

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
        ProductApiKeyState state,
        String description
) {
}
