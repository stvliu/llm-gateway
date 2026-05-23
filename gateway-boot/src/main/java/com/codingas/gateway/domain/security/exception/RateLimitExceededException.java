package com.codingas.gateway.domain.security.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 限流超限异常
 *
 * <p>表示请求超过了允许的速率限制。</p>
 */
public class RateLimitExceededException extends GatewayException {

    private static final String CODE = "RATE_LIMIT_EXCEEDED";

    public RateLimitExceededException(String message) {
        super(CODE, message);
    }

    public RateLimitExceededException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public RateLimitExceededException() {
        super(CODE, "Rate limit exceeded");
    }
}
