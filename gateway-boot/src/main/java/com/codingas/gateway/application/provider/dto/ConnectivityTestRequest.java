/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.provider.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 连通性测试请求
 *
 * @param protocolName 协议名称（必填，如 openai、anthropic）
 * @param baseUrl      供应商 Base URL（可选，使用默认值）
 * @param apiKey       待测试的 API Key（必填）
 * @param model        指定测试模型（可选，用于 Level 2 测试）
 */
public record ConnectivityTestRequest(
    @NotBlank(message = "Protocol name is required")
    String protocolName,

    String baseUrl,

    @NotBlank(message = "API Key is required")
    String apiKey,

    String model
) {}