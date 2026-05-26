package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型规格创建请求
 */
@Data
public class ModelSpecCreateRequest {

    @NotNull(message = "供应商 ID 不能为空")
    private Long providerId;

    @NotBlank(message = "供应商模型 ID 不能为空")
    @Size(max = 128)
    private String providerModelId;

    @Size(max = 128)
    private String displayName;

    @Size(max = 64)
    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private Integer priority;

    private Integer weight;
}
