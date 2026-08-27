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
package com.codingas.gateway.resilience.upstream;

import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerService;
import com.codingas.gateway.resilience.circuitbreaker.CircuitBreaker;
import com.codingas.gateway.resilience.metrics.EndpointMetricsRegistry;
import com.codingas.gateway.resilience.retry.RetryExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ResilientClientFactoryImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilientClientFactoryImpl 测试")
class ResilientClientFactoryImplTest {

    @Mock
    private ChannelEndpointCircuitBreakerService circuitBreakerService;

    @Mock
    private RetryExecutor retryExecutor;

    @Mock
    private UpstreamClient<ProtocolRequest> rawClient;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final EndpointMetricsRegistry metricsRegistry = new EndpointMetricsRegistry();

    @Test
    @DisplayName("wrap 返回包装后的韧性客户端")
    void wrap_returnsResilientClient() {
        CircuitBreaker breaker = new CircuitBreaker(0.5, 10, 30000, 3);
        when(circuitBreakerService.getBreaker(42L)).thenReturn(breaker);
        when(rawClient.supportedProvider()).thenReturn("openai");

        ResilientClientFactoryImpl factory = new ResilientClientFactoryImpl(
                circuitBreakerService, retryExecutor, meterRegistry, metricsRegistry);

        UpstreamClient<ProtocolRequest> wrapped = factory.wrap(rawClient, 42L);

        assertThat(wrapped).isInstanceOf(ResilientUpstreamClient.class);
        assertThat(wrapped.supportedProvider()).isEqualTo("openai");
        verify(circuitBreakerService).getBreaker(42L);
    }
}
