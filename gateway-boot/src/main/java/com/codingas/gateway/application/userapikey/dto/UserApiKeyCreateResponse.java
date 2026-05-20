package com.codingas.gateway.application.userapikey.dto;

/**
 * 用户 API Key 创建响应（包含仅展示一次的明文 Key）
 *
 * @param id 主键
 * @param keyPrefix Key 前缀
 * @param apiKeyPlain 明文 API Key（仅创建时返回，后续不可获取）
 */
public record UserApiKeyCreateResponse(
        Long id,
        String keyPrefix,
        String apiKeyPlain
) {
}