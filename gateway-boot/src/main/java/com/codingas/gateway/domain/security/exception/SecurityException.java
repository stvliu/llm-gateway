package com.codingas.gateway.domain.security.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 安全级异常
 *
 * <p>表示认证、授权等安全相关的错误。</p>
 */
public class SecurityException extends GatewayException {

    public SecurityException(String code, String message) {
        super(code, message);
    }

    public SecurityException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
