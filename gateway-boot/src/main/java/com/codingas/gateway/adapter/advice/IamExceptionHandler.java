package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.iam.exception.AuthenticationFailedException;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import com.codingas.gateway.domain.iam.exception.IamException;
import com.codingas.gateway.domain.iam.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * IAM 子域异常处理器
 *
 * <p>处理身份与访问控制相关的异常。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class IamExceptionHandler {

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
     * 处理 IAM 兜底异常
     */
    @ExceptionHandler(IamException.class)
    public ResponseEntity<ApiResponse<Void>> handleIamException(IamException e) {
        log.warn("IAM error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}
