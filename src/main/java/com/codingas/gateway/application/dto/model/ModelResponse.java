package com.codingas.gateway.adapter.admin.dto.model;

import com.codingas.gateway.domain.supply.entity.Model.ModelStatus;
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
    private String modelCode;
    private Long providerId;
    private String providerName;
    private String providerCode;
    private String providerModelId;
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private ModelStatus status;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
