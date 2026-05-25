package com.codingas.gateway.infrastructure.resilience;

/**
 * 熔断器开启异常
 *
 * <p>当熔断器处于 OPEN 状态时拒绝请求抛出此异常。</p>
 */
public class CircuitOpenException extends RuntimeException {

    public CircuitOpenException(String message) {
        super(message);
    }
}