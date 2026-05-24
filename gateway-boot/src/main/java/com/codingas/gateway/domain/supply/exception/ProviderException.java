package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;

/**
 * 供应商异常
 *
 * <p>表示调用外部模型供应商时发生的错误。</p>
 */
public class ProviderException extends GatewayException {

    public ProviderException(String code, String message) {
        super(code, message);
    }

    public ProviderException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}