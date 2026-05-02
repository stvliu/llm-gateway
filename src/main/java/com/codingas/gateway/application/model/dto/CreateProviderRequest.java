package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.model.entity.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建提供商请求 DTO
 */
@Data
public class CreateProviderRequest {

    @NotBlank(message = "Provider code is required")
    @Size(max = 64, message = "Provider code must not exceed 64 characters")
    private String providerCode;

    @NotBlank(message = "Provider name is required")
    @Size(max = 256, message = "Provider name must not exceed 256 characters")
    private String providerName;

    @NotNull(message = "Provider type is required")
    private String providerType;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private Integer priority = 100;
    private Boolean enabled = true;

    /**
     * 转换为实体
     */
    public Provider toEntity() {
        Provider provider = new Provider();
        provider.setProviderCode(this.providerCode);
        provider.setProviderName(this.providerName);
        provider.setProviderType(ProviderType.valueOf(this.providerType));
        provider.setBaseUrl(this.baseUrl);
        provider.setPriority(this.priority);
        provider.setStatus(this.enabled != null && this.enabled
                ? Provider.ProviderStatus.ACTIVE
                : Provider.ProviderStatus.SUSPENDED);
        return provider;
    }
}
