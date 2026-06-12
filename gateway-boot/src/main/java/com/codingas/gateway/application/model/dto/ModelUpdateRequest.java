package com.codingas.gateway.application.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

/**
 * 更新模型请求
 */
@Data
public class ModelUpdateRequest {

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private Integer contextWindow;

    private Map<String, Boolean> capabilities;
}
