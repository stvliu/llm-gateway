package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import org.springframework.stereotype.Component;

/**
 * 韧性客户端工厂实现
 *
 * <p>组合熔断器管理器和重试执行器，为原始 UpstreamClient 包装韧性保护。</p>
 */
@Component
public class ResilientClientFactoryImpl implements ResilientClientFactory {

    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final RetryExecutor retryExecutor;

    public ResilientClientFactoryImpl(ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                                       RetryExecutor retryExecutor) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public UpstreamClient wrap(UpstreamClient rawClient, Long channelEndpointId) {
        CircuitBreaker breaker = circuitBreakerManager.getBreaker(channelEndpointId);
        return new ResilientUpstreamClient(rawClient, breaker, retryExecutor);
    }
}