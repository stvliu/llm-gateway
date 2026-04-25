package com.codingas.gateway.web.dto;

import com.codingas.gateway.core.domain.entity.Model;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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
    private Map<String, Object> capabilities;
    private Model.ModelStatus status;
    private String createdAt;
    private String updatedAt;

    public static ModelResponse from(Model model) {
        return ModelResponse.builder()
                .id(model.getId())
                .modelCode(model.getModelCode())
                .displayName(model.getDisplayName())
                .providerId(model.getProviderId())
                .providerModelId(model.getProviderModelId())
                .contextWindow(model.getContextWindow())
                .inputPrice(model.getInputPrice())
                .outputPrice(model.getOutputPrice())
                .capabilities(model.getCapabilities())
                .status(model.getStatus())
                .createdAt(model.getCreatedAt() != null ? model.getCreatedAt().toString() : null)
                .updatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt().toString() : null)
                .build();
    }
}
