package com.codingas.gateway.application.model.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 模型响应
 */
@Data
public class ModelResponse {

    private Long id;
    private String modelName;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private Instant deprecatedAt;
    private String deprecationMessage;

    private Instant createdAt;
    private Instant updatedAt;
}