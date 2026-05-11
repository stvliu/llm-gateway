package com.codingas.gateway.application.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 创建供应商时嵌套的模型请求
 */
@Data
public class ModelNestedRequest {

    @NotBlank(message = "Provider model ID is required")
    @Size(max = 128, message = "Provider model ID must not exceed 128 characters")
    private String providerModelId;

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private Integer contextWindow;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Map<String, Boolean> capabilities;
}
