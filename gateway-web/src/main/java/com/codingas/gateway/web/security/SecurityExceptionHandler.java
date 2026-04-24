package com.codingas.gateway.web.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 安全异常处理器
 *
 * <p>处理安全相关异常，返回统一的 JSON 错误响应。</p>
 */
@Slf4j
@RestControllerAdvice
public class SecurityExceptionHandler {

    /**
     * 处理未认证异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException e) {
        log.warn("Authentication required: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "success", false,
                "error", "UNAUTHORIZED",
                "message", "Authentication required. Please provide a valid API Key."
            ));
    }

    /**
     * 处理权限不足异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        log.warn("Permission denied: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "success", false,
                "error", "FORBIDDEN",
                "message", "Access denied. You do not have permission to perform this action."
            ));
    }

    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailed(AuthenticationFailedException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "success", false,
                "error", "AUTHENTICATION_FAILED",
                "message", "Authentication failed. Please check your API Key."
            ));
    }

    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(Map.of(
                "success", false,
                "error", "RATE_LIMIT_EXCEEDED",
                "message", "Rate limit exceeded. Please try again later."
            ));
    }

    /**
     * 处理 IP 被封禁异常
     */
    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleIpBlocked(IpBlockedException e) {
        log.warn("IP blocked: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "success", false,
                "error", "IP_BLOCKED",
                "message", "Your IP has been temporarily blocked."
            ));
    }

    /**
     * 处理其他未预期异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "success", false,
                "error", "INTERNAL_ERROR",
                "message", "An unexpected error occurred."
            ));
    }

    // ========== Custom Security Exceptions ==========

    /**
     * 未认证异常
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    /**
     * 权限不足异常
     */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    /**
     * 认证失败异常
     */
    public static class AuthenticationFailedException extends RuntimeException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }

    /**
     * 限流异常
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * IP 被封禁异常
     */
    public static class IpBlockedException extends RuntimeException {
        public IpBlockedException(String message) {
            super(message);
        }
    }
}
