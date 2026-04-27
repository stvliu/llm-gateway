package com.codingas.gateway.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 统一 API 响应封装
 *
 * @param <T> 数据类型
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private ErrorInfo error;
    private String traceId;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setData(data);
        response.setTraceId(getCurrentTraceId());
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setData(null);
        response.setError(new ErrorInfo(code, message));
        response.setTraceId(getCurrentTraceId());
        response.setTimestamp(Instant.now().toString());
        return response;
    }

    @Getter
    @Setter
    public static class ErrorInfo {
        private String code;
        private String message;
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
        return "trace_" + System.currentTimeMillis();
    }
}
