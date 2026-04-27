package com.codingas.gateway.common.exception;

/**
 * 请求级异常
 *
 * <p>表示请求处理过程中的错误。</p>
 */
public class GatewayRequestException extends GatewayException {

    public GatewayRequestException(String code, String message) {
        super(code, message);
    }

    public GatewayRequestException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
