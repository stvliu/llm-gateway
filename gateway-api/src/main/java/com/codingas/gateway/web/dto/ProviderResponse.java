package com.codingas.gateway.web.dto;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.domain.enums.ProviderType;
import lombok.Builder;
import lombok.Data;

/**
 * 提供商响应 DTO
 */
@Data
@Builder
public class ProviderResponse {

    private Long id;
    private String providerCode;
    private String providerName;
    private ProviderType providerType;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private ProviderStatus status;
    private Integer priority;
    private String createdAt;
    private String updatedAt;

    public static ProviderResponse from(Provider provider) {
        return ProviderResponse.builder()
                .id(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType())
                .baseUrl(provider.getBaseUrl())
                .websiteUrl(provider.getWebsiteUrl())
                .apiDocUrl(provider.getApiDocUrl())
                .status(provider.getStatus())
                .priority(provider.getPriority())
                .createdAt(provider.getCreatedAt() != null ? provider.getCreatedAt().toString() : null)
                .updatedAt(provider.getUpdatedAt() != null ? provider.getUpdatedAt().toString() : null)
                .build();
    }
}