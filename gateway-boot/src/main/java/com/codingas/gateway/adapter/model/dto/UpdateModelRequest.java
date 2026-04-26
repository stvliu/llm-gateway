package com.codingas.gateway.adapter.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新模型请求 DTO
 */
@Data
public class UpdateModelRequest {

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    @Size(max = 128, message = "Provider model ID must not exceed 128 characters")
    private String providerModelId;

    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String capabilities;
    private String status;
}
