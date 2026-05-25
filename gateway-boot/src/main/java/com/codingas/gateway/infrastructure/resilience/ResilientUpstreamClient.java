package com.codingas.gateway.infrastructure.resilience;

import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.valueobject.ConnectivityTestResultVO;
import lombok.extern.java.Log;
import org.slf4j.LoggerFactory;

/**
 * 韧性 UpstreamClient 包装器
 *
 * <p>组合熔断器 + 重试策略，为上游调用提供弹性保护。</p>
 */
@Log
public class ResilientUpstreamClient implements UpstreamClient {

    private final UpstreamClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final RetryExecutor retryExecutor;

    public ResilientUpstreamClient(UpstreamClient delegate, CircuitBreaker circuitBreaker,
                                    RetryExecutor retryExecutor) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreaker;
        this.retryExecutor = retryExecutor;
    }

    @Override
    public ProtocolResponse chat(ProtocolRequest request) {
        if (!circuitBreaker.allowRequest()) {
            throw new CircuitOpenException("熔断器开启，拒绝请求");
        }

        try {
            ProtocolResponse response = retryExecutor.execute(() -> delegate.chat(request));
            circuitBreaker.recordSuccess();
            return response;
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            throw e;
        }
    }

    @Override
    public void chatStream(ProtocolRequest request, StreamCallback callback) {
        if (!circuitBreaker.allowRequest()) {
            throw new CircuitOpenException("熔断器开启，拒绝流式请求");
        }

        try {
            delegate.chatStream(request, new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    callback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    circuitBreaker.recordSuccess();
                    callback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    circuitBreaker.recordFailure();
                    callback.onError(t);
                }
            });
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            throw e;
        }
    }

    @Override
    public ConnectivityTestResultVO testConnectivity() {
        return delegate.testConnectivity();
    }
}