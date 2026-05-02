package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.domain.model.entity.Provider;
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
    private String providerType;
    private String baseUrl;
    private Integer priority;
    private Boolean enabled;
    private String createdAt;
    private String updatedAt;

    /**
     * 从实体转换为响应 DTO
     */
    public static ProviderResponse from(Provider provider) {
        if (provider == null) {
            return null;
        }
        return ProviderResponse.builder()
                .id(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType() != null ? provider.getProviderType().name() : null)
                .baseUrl(provider.getBaseUrl())
                .priority(provider.getPriority())
                .enabled(provider.getStatus() == Provider.ProviderStatus.ACTIVE)
                .createdAt(provider.getCreatedAt() != null ? provider.getCreatedAt().toString() : null)
                .updatedAt(provider.getUpdatedAt() != null ? provider.getUpdatedAt().toString() : null)
                .build();
    }
}
