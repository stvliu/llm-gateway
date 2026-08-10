/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channelcredential.dto;


import java.time.Instant;

/**
 * 渠道凭证详情响应（含明文 Key，仅单个查询时返回）
 *
 * @param id 主键
 * @param channelId 渠道 ID
 * @param apiKeyPrefix Key 前缀
 * @param apiKeyPlain 明文 API Key（仅单个查询时返回，列表查询不返回）
 * @param name 密钥名称
 * @param description 描述
 * @param weight 权重
 * @param priority 优先级
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ChannelCredentialDetailResponse(
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
