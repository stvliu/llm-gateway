/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.resilience;

/**
 * 熔断器状态
 */
public enum CircuitBreakerState {
    /** 正常放行 */
    CLOSED,
    /** 熔断开启，拒绝请求 */
    OPEN,
    /** 试探放行，允许少量请求 */
    HALF_OPEN
}