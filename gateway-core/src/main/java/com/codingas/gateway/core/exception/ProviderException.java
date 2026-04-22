package com.codingas.gateway.core.exception;

/**
 * 提供商级异常
 *
 * <p>处理上游 LLM 提供商的错误。</p>
 */
public class ProviderException extends GatewayException {

    private final String providerCode;

    public ProviderException(String providerCode, String message) {
        super("PROVIDER_ERROR", message);
        this.providerCode = providerCode;
    }

    public ProviderException(String providerCode, String message, Throwable cause) {
        super("PROVIDER_ERROR", message, cause);
        this.providerCode = providerCode;
    }

    public String getProviderCode() {
        return providerCode;
    }
}
