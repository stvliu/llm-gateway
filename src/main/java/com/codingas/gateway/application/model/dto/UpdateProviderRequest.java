package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.supply.entity.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新提供商请求 DTO
 */
@Data
public class UpdateProviderRequest {

    @NotBlank(message = "Provider name is required")
    private String providerName;

    @NotNull(message = "Provider type is required")
    private String providerType;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private Integer priority;
    private Boolean enabled;

    /**
     * 转换为实体
     */
    public Provider toEntity() {
        Provider provider = new Provider();
        provider.setProviderName(this.providerName);
        provider.setProviderType(ProviderType.valueOf(this.providerType));
        provider.setBaseUrl(this.baseUrl);
        provider.setPriority(this.priority);
        if (this.enabled != null) {
            provider.setStatus(this.enabled
                    ? Provider.ProviderStatus.ACTIVE
                    : Provider.ProviderStatus.SUSPENDED);
        }
        return provider;
    }
}
