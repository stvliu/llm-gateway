package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * API Key 连通性测试请求
 */
@Data
public class TestApiKeyRequestDTO {

    /** 供应商类型 */
    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    /** 供应商 Base URL（可选，使用默认值） */
    private String baseUrl;

    /** 待测试的 API Key */
    @NotBlank(message = "API Key is required")
    private String apiKey;
}