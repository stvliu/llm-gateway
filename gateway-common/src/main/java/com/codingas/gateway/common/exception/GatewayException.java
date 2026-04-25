package com.codingas.gateway.common.exception;

/**
 * 网关异常根类
 *
 * <p>所有网关异常必须继承此类。</p>
 */
public abstract class GatewayException extends RuntimeException {

    private final String errorCode;

    protected GatewayException(String message) {
        super(message);
        this.errorCode = "GATEWAY_ERROR";
    }

    protected GatewayException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected GatewayException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
