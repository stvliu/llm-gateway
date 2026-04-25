package com.codingas.gateway.web.dto;

import com.codingas.gateway.core.domain.entity.Model;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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

    @NotBlank(message = "Provider model ID is required")
    @Size(max = 128, message = "Provider model ID must not exceed 128 characters")
    private String providerModelId;

    private Integer contextWindow;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Map<String, Object> capabilities;

    private Model.ModelStatus status = Model.ModelStatus.ACTIVE;

    public Model toEntity() {
        Model model = new Model();
        model.setModelCode(this.modelCode);
        model.setDisplayName(this.displayName);
        model.setProviderId(this.providerId);
        model.setProviderModelId(this.providerModelId);
        model.setContextWindow(this.contextWindow);
        model.setInputPrice(this.inputPrice);
        model.setOutputPrice(this.outputPrice);
        model.setCapabilities(this.capabilities);
        model.setStatus(this.status);
        return model;
    }
}
