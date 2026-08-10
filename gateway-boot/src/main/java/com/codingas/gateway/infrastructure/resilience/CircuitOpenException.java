/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;

/**
 * 熔断器开启异常
 *
 * <p>当熔断器处于 OPEN 状态时拒绝请求抛出此异常。</p>
 */
public class CircuitOpenException extends ProviderException {

    public CircuitOpenException(String message) {
        super(ProviderErrorType.UPSTREAM_ERROR, message);
    }

    public CircuitOpenException(String traceId, String model, String provider, Long endpointId) {
        super(ProviderErrorType.UPSTREAM_ERROR, "熔断器开启，拒绝请求",
              traceId, model, provider, endpointId, null);
    }
}