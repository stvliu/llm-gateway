package com.codingas.gateway.infrastructure.upstream;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;

/**
 * SSE 错误格式化工具
 *
 * <p>将 ProviderException 格式化为结构化 SSE 错误事件。</p>
 */
public class SseErrorFormatter {

    private SseErrorFormatter() {}

    /**
     * 格式化 SSE 错误事件
     *
     * @param e ProviderException
     * @return 结构化 JSON 字符串
     */
    public static String format(ProviderException e) {
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
