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
package com.codingas.gateway.provider.channel;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * API Key 测试用例结果
 *
 * <p>承载渠道凭证连通性测试的结果（延迟、模型名、响应预览与错误信息）。</p>
 */
@Data
@Builder
public class ApiKeyTestResult {

    /** 是否测试成功 */
    private Boolean success;

    /** 测试延迟（毫秒） */
    private Long latency;

    /** 测试使用的模型名 */
    private String modelName;

    /** 响应预览 */
    private String responsePreview;

    /** 测试时间 */
    private Instant testedAt;

    /** 错误信息（失败时） */
    private ApiKeyTestError error;

    /**
     * 测试错误
     */
    @Data
    @Builder
    public static class ApiKeyTestError {
        /** 错误码 */
        private String code;
        /** 错误消息 */
        private String message;
    }
}
