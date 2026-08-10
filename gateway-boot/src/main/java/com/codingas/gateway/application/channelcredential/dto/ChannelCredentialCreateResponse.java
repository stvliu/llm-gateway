/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channelcredential.dto;

/**
 * 渠道凭证创建响应（包含仅展示一次的明文 Key）
 *
 * @param id 主键
 * @param apiKeyPlain 明文 API Key（仅创建时返回，后续不可获取）
 */
public record ChannelCredentialCreateResponse(
        Long id,
        String apiKeyPlain
) {
}
