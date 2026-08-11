/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.domain.supply.exception;

import com.codingas.gateway.common.exception.GatewayException;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

/**
 * 供应商异常
 *
 * <p>表示调用外部模型供应商时发生的错误，包含错误类型和上下文信息。</p>
 */
public class ProviderException extends GatewayException {

    private final ProviderErrorType errorType;
    private final String traceId;
    private final String model;
    private final String provider;
    private final Long channelEndpointId;
    private final Integer retryAfterSeconds;

    public ProviderException(String code, String message) {
        super(code, message);
        this.errorType = ProviderErrorType.UNKNOWN_ERROR;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(String code, String message, Throwable cause) {
        super(code, message, cause);
        this.errorType = ProviderErrorType.UNKNOWN_ERROR;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message) {
        super(errorType.name(), message);
        this.errorType = errorType;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message, Throwable cause) {
        super(errorType.name(), message, cause);
        this.errorType = errorType;
        this.traceId = null;
        this.model = null;
        this.provider = null;
        this.channelEndpointId = null;
        this.retryAfterSeconds = null;
    }

    public ProviderException(ProviderErrorType errorType, String message,
                             String traceId, String model, String provider,
                             Long channelEndpointId, Integer retryAfterSeconds) {
        super(errorType.name(), message);
        this.errorType = errorType;
        this.traceId = traceId;
        this.model = model;
        this.provider = provider;
        this.channelEndpointId = channelEndpointId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ProviderException(ProviderErrorType errorType, String message, Throwable cause,
                             String traceId, String model, String provider,
                             Long channelEndpointId, Integer retryAfterSeconds) {
        super(errorType.name(), message, cause);
        this.errorType = errorType;
        this.traceId = traceId;
        this.model = model;
        this.provider = provider;
        this.channelEndpointId = channelEndpointId;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ProviderErrorType getErrorType() { return errorType; }
    public String getTraceId() { return traceId; }
    public String getModel() { return model; }
    public String getProvider() { return provider; }
    public Long getChannelEndpointId() { return channelEndpointId; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
}
