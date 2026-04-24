package com.codingas.gateway.dispatch;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 错误响应适配器 - 根据请求来源适配错误格式
 *
 * <p>实现 OpenAI 和 Anthropic 错误格式之间的转换。</p>
 * <p>用于：当请求方期望特定提供商的错误格式时进行转换。</p>
 */
@Slf4j
public class ErrorResponseAdapter {

    /**
     * 转换为 OpenAI 错误格式
     *
     * @param errorMessage 错误消息
     * @param errorCode 错误码 (可选)
     * @param httpStatus HTTP 状态码
     * @return OpenAI 格式错误响应 Map
     */
    public Map<String, Object> toOpenAIError(String errorMessage, String errorCode, int httpStatus) {
        return Map.of(
                "error", Map.of(
                        "message", errorMessage != null ? errorMessage : "Unknown error",
                        "type", mapToOpenAIErrorType(httpStatus),
                        "code", errorCode != null ? errorCode : mapToOpenAIErrorCode(httpStatus),
                        "status", httpStatus
                )
        );
    }

    /**
     * 转换为 Anthropic 错误格式
     *
     * @param errorMessage 错误消息
     * @param errorCode 错误码 (可选)
     * @param httpStatus HTTP 状态码
     * @return Anthropic 格式错误响应 Map
     */
    public Map<String, Object> toAnthropicError(String errorMessage, String errorCode, int httpStatus) {
        return Map.of(
                "type", mapToAnthropicErrorType(httpStatus),
                "message", errorMessage != null ? errorMessage : "Unknown error",
                "code", errorCode != null ? errorCode : mapToAnthropicErrorCode(httpStatus),
                "status", httpStatus
        );
    }

    /**
     * 从 ProviderException 转换为 OpenAI 错误格式
     *
     * @param providerCode 提供商代码
     * @param errorMessage 错误消息
     * @param httpStatus HTTP 状态码
     * @return OpenAI 格式错误响应 Map
     */
    public Map<String, Object> fromProviderError(String providerCode, String errorMessage, int httpStatus) {
        // 根据提供商类型选择错误格式
        if ("anthropic".equals(providerCode)) {
            return toAnthropicError(errorMessage, null, httpStatus);
        }
        return toOpenAIError(errorMessage, null, httpStatus);
    }

    private String mapToOpenAIErrorType(int httpStatus) {
        if (httpStatus >= 500) {
            return "server_error";
        } else if (httpStatus == 429) {
            return "rate_limit_error";
        } else if (httpStatus >= 400) {
            return "invalid_request_error";
        }
        return "unknown_error";
    }

    private String mapToOpenAIErrorCode(int httpStatus) {
        if (httpStatus == 401) {
            return "invalid_api_key";
        } else if (httpStatus == 403) {
            return "permission_not_found";
        } else if (httpStatus == 404) {
            return "not_found";
        } else if (httpStatus == 429) {
            return "rate_limit_exceeded";
        } else if (httpStatus >= 500) {
            return "internal_server_error";
        } else if (httpStatus >= 400) {
            return "invalid_request";
        }
        return "unknown_error";
    }

    private String mapToAnthropicErrorType(int httpStatus) {
        if (httpStatus >= 500) {
            return "error";
        } else if (httpStatus == 429) {
            return "rate_limit_error";
        } else if (httpStatus >= 400) {
            return "invalid_request_error";
        }
        return "error";
    }

    private String mapToAnthropicErrorCode(int httpStatus) {
        if (httpStatus == 401) {
            return "authentication_error";
        } else if (httpStatus == 403) {
            return "permission_denied";
        } else if (httpStatus == 404) {
            return "not_found_error";
        } else if (httpStatus == 429) {
            return "rate_limit_exceeded";
        } else if (httpStatus >= 500) {
            return "internal_server_error";
        } else if (httpStatus >= 400) {
            return "invalid_request_error";
        }
        return "unknown_error";
    }
}
