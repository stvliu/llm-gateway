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
package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.infrastructure.supply.upstream.AnthropicUpstreamClient;
import com.codingas.gateway.infrastructure.supply.upstream.OpenAIUpstreamClient;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;
    private final EndpointMetricsRegistry metricsRegistry;

    public ResilientClientFactoryImpl(ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                                       RetryExecutor retryExecutor, MeterRegistry meterRegistry,
                                       EndpointMetricsRegistry metricsRegistry) {
        this.circuitBreakerManager = circuitBreakerManager;
        this.retryExecutor = retryExecutor;
        this.meterRegistry = meterRegistry;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public UpstreamClient wrap(UpstreamClient rawClient, Long channelEndpointId) {
        CircuitBreaker breaker = circuitBreakerManager.getBreaker(channelEndpointId);
        String providerCode = resolveProviderCode(rawClient);
        return new ResilientUpstreamClient(rawClient, breaker, retryExecutor,
                meterRegistry, metricsRegistry, providerCode, channelEndpointId);
    }

    private String resolveProviderCode(UpstreamClient client) {
        if (client instanceof OpenAIUpstreamClient) {
            return "openai";
        }
        if (client instanceof AnthropicUpstreamClient) {
            return "anthropic";
        }
        return "unknown";
    }
}