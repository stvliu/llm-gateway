package com.codingas.gateway.application.provider.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建提供商请求
 *
 * <p>注意：API Key 管理已迁移到 ProductApiKey，创建 Provider 时不再创建 API Key。</p>
 */
@Data
public class ProviderCreateRequest {

    @NotBlank(message = "Provider name is required")
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority = 100;

    /**
     * 嵌套的模型列表（可选）
     */
    @Valid
    private List<ModelNestedRequest> models;
}