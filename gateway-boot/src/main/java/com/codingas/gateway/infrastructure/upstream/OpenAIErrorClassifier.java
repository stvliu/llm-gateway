package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import org.springframework.stereotype.Component;

/**
 * OpenAI 错误分类器
 *
 * <p>根据 OpenAI API 错误响应格式，将 HTTP 状态码 + 错误体映射为 ProviderErrorType。</p>
 */
@Component
public class OpenAIErrorClassifier implements ErrorClassificationStrategy {

    private static final String PROVIDER = "openai";

    @Override
    public ProviderErrorType classify(int statusCode, String responseBody) {
        return switch (statusCode) {
            case 401 -> ProviderErrorType.AUTHENTICATION_ERROR;
            case 429 -> classifyRateLimit(responseBody);
            case 400 -> ProviderErrorType.INVALID_REQUEST;
            case 408 -> ProviderErrorType.TIMEOUT_ERROR;
            case 504 -> ProviderErrorType.TIMEOUT_ERROR;
            case 500, 502, 503 -> ProviderErrorType.UPSTREAM_ERROR;
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
