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

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.provider.upstream.UpstreamClient;
import com.codingas.gateway.provider.upstream.ConnectivityTestResult;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.java.Log;

/**
 * 韧性 UpstreamClient 包装器
 *
 * <p>组合熔断器 + 重试策略 + Metrics 埋点，为上游调用提供弹性保护。</p>
 */
@Log
public class ResilientUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final RetryExecutor retryExecutor;
    private final MeterRegistry meterRegistry;
    private final EndpointMetricsRegistry metricsRegistry;
    private final String providerCode;
    private final Long endpointId;

    public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker,
                                    RetryExecutor retryExecutor, MeterRegistry meterRegistry,
                                    EndpointMetricsRegistry metricsRegistry,
                                    String providerCode, Long endpointId) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.retryExecutor = retryExecutor;
        this.meterRegistry = meterRegistry;
        this.metricsRegistry = metricsRegistry;
        this.providerCode = providerCode;
        this.endpointId = endpointId;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
        if (!circuitBreaker.allowRequest()) {
            meterRegistry.counter("gateway.circuitbreaker.blocked",
                    "provider", providerCode,
                    "endpoint_id", String.valueOf(endpointId)).increment();
            throw new CircuitOpenException("熔断器开启，拒绝请求");
        }

        EndpointMetrics metrics = metricsRegistry.get(endpointId);
        metrics.beginCall();
        long startTime = System.currentTimeMillis();

        try {
            ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
            circuitBreaker.recordSuccess();
            metrics.endCall(System.currentTimeMillis() - startTime, true);
            return response;
        } catch (ProviderException e) {
            circuitBreaker.recordFailure();
            metrics.endCall(System.currentTimeMillis() - startTime, false);
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", e.getErrorType().name()).increment();
            throw e;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            metrics.endCall(System.currentTimeMillis() - startTime, false);
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", "UNKNOWN").increment();
            throw e;
        }
    }

    @Override
    public void chatStream(ProtocolRequest request, StreamCallback callback) {
        if (!circuitBreaker.allowRequest()) {
            meterRegistry.counter("gateway.circuitbreaker.blocked",
                    "provider", providerCode,
                    "endpoint_id", String.valueOf(endpointId)).increment();
            throw new CircuitOpenException("熔断器开启，拒绝流式请求");
        }

        EndpointMetrics metrics = metricsRegistry.get(endpointId);
        metrics.beginCall();
        long startTime = System.currentTimeMillis();

        try {
            delegate.chatStream(request, new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    callback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    circuitBreaker.recordSuccess();
                    metrics.endCall(System.currentTimeMillis() - startTime, true);
                    callback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    circuitBreaker.recordFailure();
                    metrics.endCall(System.currentTimeMillis() - startTime, false);
                    if (t instanceof ProviderException pe) {
                        meterRegistry.counter("gateway.provider.errors",
                                "provider", providerCode,
                                "error_type", pe.getErrorType().name()).increment();
                    } else {
                        meterRegistry.counter("gateway.provider.errors",
                                "provider", providerCode,
                                "error_type", "UNKNOWN").increment();
                    }
                    callback.onError(t);
                }
            });
        } catch (ProviderException e) {
            circuitBreaker.recordFailure();
            metrics.endCall(System.currentTimeMillis() - startTime, false);
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", e.getErrorType().name()).increment();
            throw e;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            metrics.endCall(System.currentTimeMillis() - startTime, false);
            meterRegistry.counter("gateway.provider.errors",
                    "provider", providerCode,
                    "error_type", "UNKNOWN").increment();
            throw e;
        }
    }

    @Override
    public ConnectivityTestResult testConnectivity() {
        return delegate.testConnectivity();
    }
}