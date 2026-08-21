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
package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.iam.exception.IamException;
import com.codingas.gateway.iam.exception.UnauthorizedException;
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
