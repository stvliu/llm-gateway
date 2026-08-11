/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
