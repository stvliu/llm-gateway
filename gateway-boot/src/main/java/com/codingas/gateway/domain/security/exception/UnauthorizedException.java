package com.codingas.gateway.domain.security.exception;

/**
 * 未认证异常
 *
 * <p>表示请求缺少有效的认证信息。</p>
 */
public class UnauthorizedException extends SecurityException {

    private static final String CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(CODE, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    public UnauthorizedException() {
        super(CODE, "Authentication required");
    }
}
