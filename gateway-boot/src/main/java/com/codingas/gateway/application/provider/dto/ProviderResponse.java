package com.codingas.gateway.application.provider.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 提供商响应
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

    /**
     * Key 统计信息（列表页填充）
     */
    private ProviderKeyStats keyStats;
}