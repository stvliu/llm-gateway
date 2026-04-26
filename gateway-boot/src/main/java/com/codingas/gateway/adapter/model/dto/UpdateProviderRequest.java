package com.codingas.gateway.adapter.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新提供商请求 DTO
 */
@Data
public class UpdateProviderRequest {

    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    private String providerType;

    @Size(max = 256, message = "Base URL must not exceed 256 characters")
    private String baseUrl;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private String status;
    private Integer priority;
}
