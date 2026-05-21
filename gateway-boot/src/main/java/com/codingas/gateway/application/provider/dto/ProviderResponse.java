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
    private String providerName;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
}