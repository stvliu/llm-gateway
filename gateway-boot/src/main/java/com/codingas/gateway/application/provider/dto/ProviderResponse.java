/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.provider.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 提供商响应
 *
 * <p>注意：Key 统计信息已移除，API Key 管理迁移到 ProductApiKey。</p>
 */
@Data
public class ProviderResponse {

    private Long id;
    /** 品牌标识（如 openai、anthropic），用于前端图标渲染 */
    private String providerId;
    private String providerName;
    private String description;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;
}