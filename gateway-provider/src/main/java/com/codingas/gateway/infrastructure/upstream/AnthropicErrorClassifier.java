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
package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.common.enums.ProviderErrorType;
import org.springframework.stereotype.Component;

/**
 * Anthropic 错误分类器
 *
 * <p>根据 Anthropic API 错误响应格式，将 HTTP 状态码 + 错误体映射为 ProviderErrorType。</p>
 */
@Component
public class AnthropicErrorClassifier implements ErrorClassificationStrategy {

    private static final String PROVIDER = "anthropic";

    @Override
    public ProviderErrorType classify(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 401 -> ProviderErrorType.AUTHENTICATION_ERROR;
            case 429 -> classifyRateLimit(responseBody);
            case 400 -> ProviderErrorType.INVALID_REQUEST;
            case 408 -> ProviderErrorType.TIMEOUT_ERROR;
            case 504 -> ProviderErrorType.TIMEOUT_ERROR;
            case 500, 502, 529 -> ProviderErrorType.UPSTREAM_ERROR;
            case 503 -> ProviderErrorType.SERVICE_UNAVAILABLE;
            default -> ProviderErrorType.UNKNOWN_ERROR;
        };
    }

    private ProviderErrorType classifyRateLimit(String responseBody) {
        if (responseBody == null) return ProviderErrorType.RATE_LIMIT_ERROR;
        String lower = responseBody.toLowerCase();
        if (lower.contains("quota") || lower.contains("insufficient_quota")) {
            return ProviderErrorType.QUOTA_EXCEEDED;
        }
        return ProviderErrorType.RATE_LIMIT_ERROR;
    }

    @Override
    public String supportedProvider() {
        return PROVIDER;
    }
}
