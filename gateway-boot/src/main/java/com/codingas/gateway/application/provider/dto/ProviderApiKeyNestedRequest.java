package com.codingas.gateway.application.provider.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建供应商时嵌套的 API Key 请求
 */
@Data
/**
 * @deprecated 旧架构 DTO
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ProviderApiKeyNestedRequest {

    @NotBlank(message = "Key name is required")
    @Size(max = 128, message = "Key name must not exceed 128 characters")
    private String keyName;

    @NotBlank(message = "API Key is required")
    private String apiKey;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 1000, message = "Priority must not exceed 1000")
    private Integer priority = 100;

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 1000, message = "Weight must not exceed 1000")
    private Integer weight = 100;

    private Boolean isDefault = false;
}
