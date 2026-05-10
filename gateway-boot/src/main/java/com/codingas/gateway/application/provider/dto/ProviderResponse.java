package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.domain.model.enums.ProviderState;
import com.codingas.gateway.domain.model.enums.ProviderType;
import lombok.Data;

import java.time.Instant;

/**
 * 提供商响应
 */
@Data
public class ProviderResponse {

    private Long id;
    private String providerName;
    private ProviderType providerType;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private ProviderState state;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Key 统计信息（列表页填充）
     */
    private ProviderKeyStats keyStats;
}