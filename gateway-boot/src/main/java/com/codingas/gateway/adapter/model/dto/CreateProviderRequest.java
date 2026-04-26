package com.codingas.gateway.adapter.model.dto;

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
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @NotNull(message = "Provider type is required")
    private String providerType;

    @Size(max = 256, message = "Base URL must not exceed 256 characters")
    private String baseUrl;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private String status = "ACTIVE";
    private Integer priority = 100;
}
