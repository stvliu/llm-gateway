package com.codingas.gateway.web.dto;

import com.codingas.gateway.core.domain.entity.Model;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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

    private Map<String, Object> capabilities;

    private Model.ModelStatus status;

    public Model toEntity() {
        Model model = new Model();
        model.setDisplayName(this.displayName);
        model.setProviderModelId(this.providerModelId);
        model.setContextWindow(this.contextWindow);
        model.setInputPrice(this.inputPrice);
        model.setOutputPrice(this.outputPrice);
        model.setCapabilities(this.capabilities);
        model.setStatus(this.status);
        return model;
    }
}
