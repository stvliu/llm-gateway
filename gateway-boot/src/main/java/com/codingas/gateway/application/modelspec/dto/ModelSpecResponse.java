package com.codingas.gateway.application.modelspec.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 模型规格响应
 */
@Data
public class ModelSpecResponse {

    private Long id;

    private Long providerId;

    private String providerModelId;

    private String displayName;

    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    private String state;

    private Integer priority;

    private Integer weight;

    private Instant createdAt;

    private Instant updatedAt;
}
