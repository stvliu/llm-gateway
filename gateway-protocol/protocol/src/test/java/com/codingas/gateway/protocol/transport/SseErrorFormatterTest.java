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
package com.codingas.gateway.protocol.transport;

import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SSE 错误格式化测试")
class SseErrorFormatterTest {

    @Test
    @DisplayName("RATE_LIMIT_ERROR 格式化为 rate_limit 含 retry_after")
    void rateLimit_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.RATE_LIMIT_ERROR, "限流",
                null, null, null, null, 30);
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"rate_limit\",\"retry_after\":30}");
    }

    @Test
    @DisplayName("QUOTA_EXCEEDED 格式化为 quota_exceeded")
    void quotaExceeded_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.QUOTA_EXCEEDED, "配额超限");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"quota_exceeded\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("AUTHENTICATION_ERROR 格式化为 authentication_error")
    void authenticationError_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.AUTHENTICATION_ERROR, "认证失败");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"authentication_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("TIMEOUT_ERROR 格式化为 timeout")
    void timeout_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.TIMEOUT_ERROR, "超时");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"timeout\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("UPSTREAM_ERROR 格式化为 api_error")
    void upstreamError_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "上游错误");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"api_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("SERVICE_UNAVAILABLE 格式化为 server_error")
    void serviceUnavailable_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.SERVICE_UNAVAILABLE, "服务不可用");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"server_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("NETWORK_ERROR 格式化为 network_error")
    void networkError_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.NETWORK_ERROR, "网络异常");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"network_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("INVALID_REQUEST 格式化为 invalid_request_error")
    void invalidRequest_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.INVALID_REQUEST, "无效请求");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"invalid_request_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("MODEL_NOT_FOUND 格式化为 model_not_found")
    void modelNotFound_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.MODEL_NOT_FOUND, "模型不存在");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"model_not_found\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("UNKNOWN_ERROR 格式化为 unknown_error")
    void unknownError_format() {
        UpstreamException e = new UpstreamException(ProviderErrorType.UNKNOWN_ERROR, "未知错误");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"unknown_error\",\"retry_after\":0}");
    }

    @Test
    @DisplayName("retryAfterSeconds 为 null 时输出 0")
    void retryAfterNull_outputZero() {
        UpstreamException e = new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "错误");
        assertThat(SseErrorFormatter.format(e))
                .isEqualTo("{\"error\":\"api_error\",\"retry_after\":0}");
    }
}
