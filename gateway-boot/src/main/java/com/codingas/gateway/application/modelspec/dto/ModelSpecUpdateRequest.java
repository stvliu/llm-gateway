package com.codingas.gateway.application.modelspec.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型规格更新请求
 */
@Data
public class ModelSpecUpdateRequest {

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

    private String state;
}
