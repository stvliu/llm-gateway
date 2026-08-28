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
package com.codingas.gateway.web.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.codingas.gateway.resilience.circuitbreaker.CircuitOpenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求级异常
     */
    @ExceptionHandler(GatewayRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleGatewayRequestException(GatewayRequestException ex) {
        log.warn("Gateway request error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理熔断器开启异常
     */
    @ExceptionHandler(CircuitOpenException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitOpenException(CircuitOpenException ex) {
        log.warn("Circuit breaker open: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理提供商异常
     */
    @ExceptionHandler(UpstreamException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpstreamException(UpstreamException ex) {
        log.error("Provider error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理网关异常
     */
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ApiResponse<Void>> handleGatewayException(GatewayException ex) {
        log.error("Gateway error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * 处理资源未找到异常（Actuator 端点等不被 SPA fallback 覆盖）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    /**
     * 处理参数非法异常（配置项不存在/不可编辑/值格式错误等）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
