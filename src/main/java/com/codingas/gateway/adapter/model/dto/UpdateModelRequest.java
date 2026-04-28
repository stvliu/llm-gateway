package com.codingas.gateway.adapter.model.dto;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 更新模型请求 DTO
 */
@Data
public class UpdateModelRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Provider ID is required")
    private Long providerId;

    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;
    private String status;

    /**
     * 转换为实体
     */
    public Model toEntity() {
        Model model = new Model();
        model.setDisplayName(this.displayName);
        Provider providerEntity = new Provider();
        providerEntity.setId(this.providerId);
        model.setProvider(providerEntity);
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
