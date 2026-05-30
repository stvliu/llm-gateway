package com.codingas.gateway.application.model.dto;

import com.codingas.gateway.domain.supply.enums.ModelState;
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
    private ModelState state;

    private Instant createdAt;
    private Instant updatedAt;
}