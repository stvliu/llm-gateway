/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新模型请求
 *
 * <p>字段为 null 表示不更新该字段。</p>
 */
@Data
public class ModelUpdateRequest {

    /** 供应商侧模型标识（如 gpt-4o），为 null 表示不更新 */
    @Size(max = 128, message = "Model name must not exceed 128 characters")
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
