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

/**
 * SSE 错误格式化工具
 *
 * <p>将 UpstreamException 格式化为结构化 SSE 错误事件。</p>
 */
public class SseErrorFormatter {

    private SseErrorFormatter() {}

    /**
     * 格式化 SSE 错误事件
     *
     * @param e UpstreamException
     * @return 结构化 JSON 字符串
     */
    public static String format(UpstreamException e) {
        String type = switch (e.getErrorType()) {
            case RATE_LIMIT_ERROR -> "rate_limit";
            case QUOTA_EXCEEDED -> "quota_exceeded";
            case AUTHENTICATION_ERROR -> "authentication_error";
            case TIMEOUT_ERROR -> "timeout";
            case UPSTREAM_ERROR -> "api_error";
            case SERVICE_UNAVAILABLE -> "server_error";
            case NETWORK_ERROR -> "network_error";
            case INVALID_REQUEST -> "invalid_request_error";
            case UNKNOWN_ERROR -> "unknown_error";
        };
        int retryAfter = e.getRetryAfterSeconds() != null ? e.getRetryAfterSeconds() : 0;
        return String.format("{\"error\":\"%s\",\"retry_after\":%d}", type, retryAfter);
    }
}
