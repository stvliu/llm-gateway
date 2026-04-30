package com.codingas.gateway.adapter.admin.dto.provider;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.supply.entity.Provider.ProviderStatus;
import lombok.Data;

import java.time.Instant;

/**
 * 提供商响应
 */
@Data
public class ProviderResponse {

    private Long id;
    private String providerCode;
    private String providerName;
    private ProviderType providerType;
    private String baseUrl;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private ProviderStatus status;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
