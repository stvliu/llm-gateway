package com.codingas.gateway.application.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建模型请求
 */
@Data
public class ModelCreateRequest {

    @NotBlank(message = "Provider model ID is required")
    @Size(max = 128, message = "Provider model ID must not exceed 128 characters")
    private String modelName;

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    /** 模型族（如 gpt-4） */
    private String modelFamily;

    private Integer contextWindow;

    /** 最大输入 Token 数 */
    private Integer maxInputTokens;

    /** 最大输出 Token 数 */
    private Integer maxOutputTokens;

    private Map<String, Boolean> capabilities;

    /** 支持的模态（如 text、image、audio） */
    private List<String> modalities;
}
