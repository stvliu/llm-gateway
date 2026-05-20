package com.codingas.gateway.application.productapikey.dto;

/**
 * 产品 API Key 创建响应（包含仅展示一次的明文 Key）
 *
 * @param id 主键
 * @param apiKeyMasked 脱敏后的 API Key
 * @param apiKeyPlain 明文 API Key（仅创建时返回，后续不可获取）
 */
public record ProductApiKeyCreateResponse(
        Long id,
        String apiKeyMasked,
        String apiKeyPlain
) {
}
