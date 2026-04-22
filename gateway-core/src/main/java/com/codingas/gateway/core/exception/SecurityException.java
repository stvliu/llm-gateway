package com.codingas.gateway.core.exception;

/**
 * 安全级异常
 *
 * <p>处理认证、授权、安全等相关错误。</p>
 */
public class SecurityException extends GatewayException {

    public SecurityException(String message) {
        super("SECURITY_ERROR", message);
    }

    public SecurityException(String message, Throwable cause) {
        super("SECURITY_ERROR", message, cause);
    }
}
