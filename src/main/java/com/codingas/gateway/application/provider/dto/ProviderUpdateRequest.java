package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.common.enums.ProviderType;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提供商请求
 */
@Data
public class ProviderUpdateRequest {

    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    private ProviderType providerType;

    @Size(max = 256, message = "Base URL must not exceed 256 characters")
    private String baseUrl;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority;

    private Boolean enabled;
}
