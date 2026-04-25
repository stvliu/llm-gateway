package com.codingas.gateway.core.exception;

import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.core.domain.enums.ProviderErrorType;

/**
 * 提供商级异常
 *
 * <p>处理上游 LLM 提供商的错误。</p>
 */
public class ProviderException extends GatewayException {

    private final String providerCode;
    private final String modelId;
    private final ProviderErrorType errorType;
    private final boolean retryable;

    public ProviderException(String providerCode, String message,
                            ProviderErrorType errorType, boolean retryable) {
        super("PROVIDER_ERROR", message);
        this.providerCode = providerCode;
        this.modelId = null;
        this.errorType = errorType;
        this.retryable = retryable;
    }

    public ProviderException(String providerCode, String modelId, String message,
                            ProviderErrorType errorType, boolean retryable) {
        super("PROVIDER_ERROR", message);
        this.providerCode = providerCode;
        this.modelId = modelId;
        this.errorType = errorType;
        this.retryable = retryable;
    }

    public ProviderException(String providerCode, String message,
                            ProviderErrorType errorType, boolean retryable, Throwable cause) {
        super("PROVIDER_ERROR", message, cause);
        this.providerCode = providerCode;
        this.modelId = null;
        this.errorType = errorType;
        this.retryable = retryable;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getModelId() {
        return modelId;
    }

    public ProviderErrorType getErrorType() {
        return errorType;
    }

    /**
     * 是否可重试
     *
     * <p>对于 retryable = true 的错误，路由层可以尝试其他 Channel/Key。</p>
     */
    public boolean isRetryable() {
        return retryable;
    }
}
