package com.codingas.gateway.application.provider.dto;

import com.codingas.gateway.domain.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 连通性测试请求
 *
 * @param providerType 供应商类型（必填）
 * @param baseUrl      供应商 Base URL（可选，使用默认值）
 * @param apiKey       待测试的 API Key（必填）
 * @param model        指定测试模型（可选，用于 Level 2 测试）
 */
public record ConnectivityTestRequest(
    @NotNull(message = "Provider type is required")
    ProviderType providerType,

    String baseUrl,

    @NotBlank(message = "API Key is required")
    String apiKey,

    String model
) {}
