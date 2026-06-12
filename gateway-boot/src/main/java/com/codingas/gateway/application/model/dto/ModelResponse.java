package com.codingas.gateway.application.model.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 模型响应
 */
@Data
public class ModelResponse {

    private Long id;
    private String modelName;
    private String displayName;
    private Integer contextWindow;
    private Map<String, Boolean> capabilities;
    private Instant deprecatedAt;
    private String deprecationMessage;

    private Instant createdAt;
    private Instant updatedAt;
}