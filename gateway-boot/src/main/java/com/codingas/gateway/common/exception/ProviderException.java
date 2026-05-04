package com.codingas.gateway.common.exception;

/**
 * 提供商级异常
 *
 * <p>表示 AI 服务提供商相关的错误。</p>
 */
public class ProviderException extends GatewayException {

    public ProviderException(String code, String message) {
        super(code, message);
    }

    public ProviderException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
