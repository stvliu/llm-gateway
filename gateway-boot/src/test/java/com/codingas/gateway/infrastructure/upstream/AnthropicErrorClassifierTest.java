/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Anthropic 错误分类器测试")
class AnthropicErrorClassifierTest {

    private AnthropicErrorClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new AnthropicErrorClassifier();
    }

    @Nested
    @DisplayName("按 HTTP 状态码分类")
    class ByStatusCode {
        @Test
        @DisplayName("401 → AUTHENTICATION_ERROR")
        void status401_authenticationError() {
            assertThat(classifier.classify(401, "{}")).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
        }

        @Test
        @DisplayName("429 → RATE_LIMIT_ERROR")
        void status429_rateLimit() {
            assertThat(classifier.classify(429, "{}")).isEqualTo(ProviderErrorType.RATE_LIMIT_ERROR);
        }

        @Test
        @DisplayName("429 + quota 关键字 → QUOTA_EXCEEDED")
        void status429_withQuota_quotaExceeded() {
            assertThat(classifier.classify(429, "{\"error\":{\"message\":\"You exceeded your current quota\"}}"))
                    .isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
        }

        @Test
        @DisplayName("429 + insufficient_quota 关键字 → QUOTA_EXCEEDED")
        void status429_withInsufficientQuota_quotaExceeded() {
            assertThat(classifier.classify(429, "{\"error\":{\"message\":\"insufficient_quota\"}}"))
                    .isEqualTo(ProviderErrorType.QUOTA_EXCEEDED);
        }

        @Test
        @DisplayName("400 → INVALID_REQUEST")
        void status400_invalidRequest() {
            assertThat(classifier.classify(400, "{}")).isEqualTo(ProviderErrorType.INVALID_REQUEST);
        }

        @Test
        @DisplayName("408 → TIMEOUT_ERROR")
        void status408_timeoutError() {
            assertThat(classifier.classify(408, "{}")).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
        }

        @Test
        @DisplayName("500 → UPSTREAM_ERROR")
        void status500_upstreamError() {
            assertThat(classifier.classify(500, "{}")).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("502 → UPSTREAM_ERROR")
        void status502_upstreamError() {
            assertThat(classifier.classify(502, "{}")).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("503 → SERVICE_UNAVAILABLE")
        void status503_serviceUnavailable() {
            assertThat(classifier.classify(503, "{}")).isEqualTo(ProviderErrorType.SERVICE_UNAVAILABLE);
        }

        @Test
        @DisplayName("504 → TIMEOUT_ERROR")
        void status504_timeoutError() {
            assertThat(classifier.classify(504, "{}")).isEqualTo(ProviderErrorType.TIMEOUT_ERROR);
        }

        @Test
        @DisplayName("529 (Anthropic 过载) → UPSTREAM_ERROR")
        void status529_upstreamError() {
            assertThat(classifier.classify(529, "{}")).isEqualTo(ProviderErrorType.UPSTREAM_ERROR);
        }

        @Test
        @DisplayName("499 → UNKNOWN_ERROR")
        void status499_unknown() {
            assertThat(classifier.classify(499, "{}")).isEqualTo(ProviderErrorType.UNKNOWN_ERROR);
        }
    }

    @Nested
    @DisplayName("supportedProvider")
    class SupportedProvider {
        @Test
        @DisplayName("返回 anthropic")
        void returnsAnthropic() {
            assertThat(classifier.supportedProvider()).isEqualTo("anthropic");
        }
    }
}
