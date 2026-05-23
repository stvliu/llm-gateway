package com.codingas.gateway.application.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 创建模型请求
 */
@Data
public class ModelCreateRequest {
    @NotNull(message = "Provider ID is required")
    private Long providerId;

    @NotBlank(message = "Provider model ID is required")
    @Size(max = 128, message = "Provider model ID must not exceed 128 characters")
    private String providerModelId;

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private Integer contextWindow;

    private Map<String, Boolean> capabilities;

    /**
     * 渠道优先级（用于 FAILOVER 策略，值越小越优先，默认 100）
     */
    private Integer priority;

    /**
     * 渠道权重（用于 WEIGHTED 策略，加权随机选择，默认 100）
     */
    private Integer weight;
}
