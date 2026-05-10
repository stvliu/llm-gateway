package com.codingas.gateway.application.providerapikey.dto;

import com.codingas.gateway.domain.model.enums.ProviderApiKeyState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

/**
 * 更新 Provider API Key 请求
 */
@Data
public class ProviderApiKeyUpdateRequest {

    @Size(max = 128, message = "Key name must not exceed 128 characters")
    private String keyName;

    @Size(max = 2000, message = "API Key must not exceed 2000 characters")
    private String apiKey;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 1000, message = "Priority must not exceed 1000")
    private Integer priority;

    @Min(value = 1, message = "Weight must be at least 1")
    @Max(value = 1000, message = "Weight must not exceed 1000")
    private Integer weight;

    private Boolean isDefault;

    private ProviderApiKeyState state;

    private Instant expiresAt;

    @Min(value = 1, message = "RPM limit must be at least 1")
    private Integer rpmLimit;

    @Min(value = 1, message = "TPM limit must be at least 1")
    private Long tpmLimit;
}