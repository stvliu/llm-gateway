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
package com.codingas.gateway.protocol.gemini;

import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GeminiErrorClassifier 测试 — 按 HTTP 状态码 + 错误体映射 ProviderErrorType
 */
class GeminiErrorClassifierTest {

    private final GeminiErrorClassifier classifier = new GeminiErrorClassifier();

    @ParameterizedTest
    @CsvSource({
            "401, AUTHENTICATION_ERROR",
            "403, AUTHENTICATION_ERROR",
            "429, RATE_LIMIT_ERROR",
            "400, INVALID_REQUEST",
            "404, MODEL_NOT_FOUND",
            "408, TIMEOUT_ERROR",
            "504, TIMEOUT_ERROR",
            "500, UPSTREAM_ERROR",
            "502, UPSTREAM_ERROR",
            "503, SERVICE_UNAVAILABLE",
            "529, UPSTREAM_ERROR",
            "599, UNKNOWN_ERROR"
    })
    void classify_按状态码分类(int statusCode, ProviderErrorType expected) {
        assertThat(classifier.classify(statusCode, "{\"error\":{\"code\":" + statusCode + "}}"))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("429 且含 quota 关键词 → QUOTA_EXCEEDED")
    void classify_429含quota_映射QUOTA_EXCEEDED() {
        assertThat(classifier.classify(429, "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"quota exceeded\"}}"))
                .isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("404 且含 model 关键词 → MODEL_NOT_FOUND")
    void classify_404含model_映射MODEL_NOT_FOUND() {
        assertThat(classifier.classify(404, "{\"error\":{\"status\":\"NOT_FOUND\",\"message\":\"models/gemini-2.5-pro not found\"}}"))
                .isEqualTo(ProviderErrorType.MODEL_NOT_FOUND);
    }

    @Test
    void supportedProvider_返回gemini() {
        assertThat(classifier.supportedProvider()).isEqualTo("gemini");
    }
}
