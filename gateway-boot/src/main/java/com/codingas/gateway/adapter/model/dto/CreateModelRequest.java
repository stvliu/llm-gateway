package com.codingas.gateway.adapter.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建模型请求 DTO
 */
@Data
public class CreateModelRequest {

    @NotBlank(message = "Model code is required")
    @Size(max = 128, message = "Model code must not exceed 128 characters")
    private String modelCode;

    @NotBlank(message = "Display name is required")
    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    private String providerModelId;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String capabilities;
    private String status = "ACTIVE";
}
