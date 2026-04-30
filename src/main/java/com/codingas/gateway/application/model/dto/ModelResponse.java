package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.domain.supply.entity.Model;
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
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String status;
    private String createdAt;
    private String updatedAt;

    /**
     * 从实体转换为响应 DTO
     */
    public static ModelResponse from(Model model) {
        if (model == null) {
            return null;
        }
        return ModelResponse.builder()
                .id(model.getId())
                .modelCode(model.getModelCode())
                .displayName(model.getDisplayName())
                .providerId(model.getProvider() != null ? model.getProvider().getId() : null)
                .contextWindow(model.getContextWindow())
                .inputPrice(model.getInputPrice())
                .outputPrice(model.getOutputPrice())
                .status(model.getStatus() != null ? model.getStatus().name() : null)
                .createdAt(model.getCreatedAt() != null ? model.getCreatedAt().toString() : null)
                .updatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt().toString() : null)
                .build();
    }
}
