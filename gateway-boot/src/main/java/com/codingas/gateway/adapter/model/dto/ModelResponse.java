package com.codingas.gateway.adapter.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型响应 DTO
 */
@Data
@Builder
public class ModelResponse {

    private Long id;
    private String modelCode;
    private String displayName;
    private Long providerId;
    private String providerModelId;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String status;
    private String createdAt;
    private String updatedAt;
}
