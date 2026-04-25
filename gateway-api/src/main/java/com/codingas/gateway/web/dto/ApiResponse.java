package com.codingas.gateway.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 统一 API 响应封装
 *
 * <p>用于 /api/v1/* 管理 API 的响应格式。</p>
 *
 * @param <T> 数据类型
 * @see <a href="https://docs.llm-gateway.dev/api#api-response-format">API 响应格式</a>
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 成功时返回数据，失败时为 null
     */
    private T data;

    /**
     * 失败时包含错误信息，成功时为 null
     */
    private ErrorInfo error;

    /**
     * OpenTelemetry 追踪 ID
     */
    private String traceId;

    /**
     * ISO 8601 时间戳
     */
    private String timestamp;

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTraceId(getCurrentTraceId());
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    /**
     * 创建成功响应 (无数据)
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setData(null);
        response.setError(new ErrorInfo(code, message));
        response.setTraceId(getCurrentTraceId());
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    /**
     * 错误信息
     */
    @Getter
    @Setter
    public static class ErrorInfo {
        /**
         * 错误码
         */
        private String code;

        /**
         * 错误消息
         */
        private String message;

        /**
         * 错误详情
         */
        private Object details;

        public ErrorInfo(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorInfo(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
    }

    private static String getCurrentTraceId() {
        // 从 MDC 或其他追踪上下文获取
        return "trace_" + System.currentTimeMillis();
    }
}
