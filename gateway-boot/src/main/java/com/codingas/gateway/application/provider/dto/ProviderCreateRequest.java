package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建提供商请求
 */
@Data
public class ProviderCreateRequest {

    @NotBlank(message = "Provider name is required")
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    @Size(max = 256, message = "Base URL must not exceed 256 characters")
    private String baseUrl;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority = 100;

    /**
     * 嵌套的 API Key 列表（可选）
     */
    @Valid
    private List<ProviderApiKeyNestedRequest> apiKeys;

    /**
     * 嵌套的模型列表（可选）
     */
    @Valid
    private List<ModelNestedRequest> models;
}
