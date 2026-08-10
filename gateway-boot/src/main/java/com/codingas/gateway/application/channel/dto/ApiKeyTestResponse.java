/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.channel.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * API Key 测试响应
 */
@Data
@Builder
public class ApiKeyTestResponse {

    /** 测试是否成功 */
    private Boolean success;

    /** 延迟（毫秒） */
    private Long latency;

    /** 测试的模型名称 */
    private String modelName;

    /** 响应预览 */
    private String responsePreview;

    /** 测试时间 */
    private Instant testedAt;

    /** 错误信息 */
    private ApiKeyTestError error;

    @Data
    @Builder
    public static class ApiKeyTestError {
        private String code;
        private String message;
    }
}
