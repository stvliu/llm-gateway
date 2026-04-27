package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.domain.router.entity.Model;
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

    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private String capabilities;
    private String status = "ACTIVE";

    /**
     * 转换为实体
     */
    public Model toEntity() {
        Model model = new Model();
        model.setModelCode(this.modelCode);
        model.setDisplayName(this.displayName);
        model.setProviderId(this.providerId);
        model.setContextWindow(this.contextWindow);
        model.setInputPrice(this.inputPrice);
        model.setOutputPrice(this.outputPrice);
        model.setCapabilities(this.capabilities);
        if (this.status != null) {
            model.setStatus(Model.ModelStatus.valueOf(this.status));
        }
        return model;
    }
}
