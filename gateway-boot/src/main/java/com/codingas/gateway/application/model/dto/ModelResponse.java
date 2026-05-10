package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.domain.model.enums.ModelState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 模型响应
 */
@Data
public class ModelResponse {

    private Long id;
    private Long providerId;
    private String providerName;
    private String providerModelId;
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private ModelState state;
    private Instant createdAt;
    private Instant updatedAt;
}