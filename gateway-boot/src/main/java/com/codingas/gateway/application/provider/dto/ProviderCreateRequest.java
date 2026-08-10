/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.provider.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建提供商请求
 */
@Data
public class ProviderCreateRequest {

    /** 品牌标识（如 openai、anthropic），全局唯一 */
    @NotBlank(message = "品牌标识不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$", message = "品牌标识只能包含小写字母、数字和中划线，长度3-64")
    private String code;

    @NotBlank(message = "Provider name is required")
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority = 100;

    /**
     * 嵌套的模型列表（可选）
     */
    @Valid
    private List<ModelNestedRequest> models;
}