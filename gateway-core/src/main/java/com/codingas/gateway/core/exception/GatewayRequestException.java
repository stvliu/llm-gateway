package com.codingas.gateway.core.exception;

/**
 * 请求级异常
 *
 * <p>处理请求参数、格式等错误。</p>
 */
public class GatewayRequestException extends GatewayException {

    public GatewayRequestException(String message) {
        super("INVALID_REQUEST", message);
    }

    public GatewayRequestException(String message, Throwable cause) {
        super("INVALID_REQUEST", message, cause);
    }
}
