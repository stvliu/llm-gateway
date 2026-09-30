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
import com.codingas.gateway.protocol.transport.ErrorClassificationStrategy;

/**
 * Gemini 错误分类器
 *
 * <p>根据 Gemini API 错误响应格式（{@code {"error":{"code":429,"status":"RESOURCE_EXHAUSTED",...}}}），
 * 将 HTTP 状态码 + 错误体映射为 ProviderErrorType。
 * 由 {@code GeminiUpstreamClientFactory} 自建实例（非 Spring Bean），与协议插件自包含一致。</p>
 */
public class GeminiErrorClassifier implements ErrorClassificationStrategy {

    private static final String PROVIDER = "gemini";

    @Override
    public ProviderErrorType classify(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 401, 403 -> ProviderErrorType.AUTHENTICATION_ERROR;
            case 429 -> classifyRateLimit(responseBody);
            case 400 -> ProviderErrorType.INVALID_REQUEST;
            // Gemini 404 几乎均源于模型不存在（其余资源走不同 API），直接映射 MODEL_NOT_FOUND
            case 404 -> ProviderErrorType.MODEL_NOT_FOUND;
            case 408, 504 -> ProviderErrorType.TIMEOUT_ERROR;
            case 500, 502, 529 -> ProviderErrorType.UPSTREAM_ERROR;
            case 503 -> ProviderErrorType.SERVICE_UNAVAILABLE;
            default -> ProviderErrorType.UNKNOWN_ERROR;
        };
    }

    private ProviderErrorType classifyRateLimit(String responseBody) {
        if (responseBody == null) {
            return ProviderErrorType.RATE_LIMIT_ERROR;
        }
        String lower = responseBody.toLowerCase();
        if (lower.contains("quota") || lower.contains("resource_exhausted")) {
            return ProviderErrorType.QUOTA_EXCEEDED;
        }
        return ProviderErrorType.RATE_LIMIT_ERROR;
    }

    @Override
    public String supportedProvider() {
        return PROVIDER;
    }
}
