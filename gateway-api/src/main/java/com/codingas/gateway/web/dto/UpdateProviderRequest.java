package com.codingas.gateway.web.dto;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.domain.enums.ProviderType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提供商请求 DTO
 */
@Data
public class UpdateProviderRequest {

    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    private ProviderType providerType;

    @Size(max = 256, message = "Base URL must not exceed 256 characters")
    private String baseUrl;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private ProviderStatus status;

    private Integer priority;

    public Provider toEntity() {
        Provider provider = new Provider();
        provider.setProviderName(this.providerName);
        provider.setProviderType(this.providerType);
        provider.setBaseUrl(this.baseUrl);
        provider.setWebsiteUrl(this.websiteUrl);
        provider.setApiDocUrl(this.apiDocUrl);
        provider.setStatus(this.status);
        provider.setPriority(this.priority);
        return provider;
    }
}