package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.threat.exception.IpBlockedException;
import com.codingas.gateway.domain.threat.exception.RateLimitExceededException;
import com.codingas.gateway.domain.threat.exception.ThreatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Threat 子域异常处理器
 *
 * <p>处理限流与IP封禁相关的异常。</p>
 */
@Slf4j
@RestControllerAdvice
@Order(2)
public class ThreatExceptionHandler {

    /**
     * 处理限流超限异常
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(e.getCode(), "请求过于频繁，请稍后重试"));
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
     * 处理 Threat 兜底异常
     */
    @ExceptionHandler(ThreatException.class)
    public ResponseEntity<ApiResponse<Void>> handleThreatException(ThreatException e) {
        log.warn("Threat error: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }
}