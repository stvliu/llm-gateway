package com.codingas.gateway.adapter.advice;

import com.codingas.gateway.domain.threat.exception.IpBlockedException;
import com.codingas.gateway.domain.threat.exception.RateLimitExceededException;
import com.codingas.gateway.domain.threat.exception.ThreatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Threat 子域异常处理器测试
 */
class ThreatExceptionHandlerTest {

    private final ThreatExceptionHandler handler = new ThreatExceptionHandler();

    @Test
    @DisplayName("处理限流超限异常 - 返回429")
    void handleRateLimitExceeded() {
        RateLimitExceededException e = new RateLimitExceededException();
        ResponseEntity<?> response = handler.handleRateLimitExceeded(e);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    @DisplayName("处理 IP 封禁异常 - 返回403")
    void handleIpBlocked() {
        IpBlockedException e = new IpBlockedException();
        ResponseEntity<?> response = handler.handleIpBlocked(e);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("处理 Threat 兜底异常 - 返回403")
    void handleThreatException() {
        ThreatException e = new ThreatException("THREAT_ERROR", "威胁防护错误");
        ResponseEntity<?> response = handler.handleThreatException(e);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}