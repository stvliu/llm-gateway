package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.domain.threat.exception.IpBlockedException;
import com.codingas.gateway.domain.threat.exception.RateLimitExceededException;
import com.codingas.gateway.domain.iam.exception.IamException;
import com.codingas.gateway.domain.iam.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 安全异常处理器
 *
 * <p>处理所有安全相关的异常，统一返回 ApiResponse 格式。</p>
 * <p>使用 @Order(1) 确保优先于 GlobalExceptionHandler 处理安全异常。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class SecurityExceptionHandler {

    /**
     * 处理未认证异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException e) {
        log.warn("Authentication required: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.getCode(), "Authentication required. Please provide a valid API Key."));
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException e) {
        log.warn("Permission denied: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), "Access denied. You do not have permission to perform this action."));
    }

    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationFailed(AuthenticationFailedException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * 处理限流超限异常
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(e.getCode(), "Rate limit exceeded. Please try again later."));
    }

    /**
     * 处理 IP 封禁异常
     */
    @ExceptionHandler(IpBlockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleIpBlocked(IpBlockedException e) {
        log.warn("IP blocked: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), "Your IP has been temporarily blocked."));
    }

    /**
     * 处理通用安全异常（兜底）
     */
    @ExceptionHandler(IamException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(IamException e) {
        log.warn("Security error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}
