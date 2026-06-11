package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.proxy.routing.CredentialResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Key 级故障转移 Invoker
 *
 * <p>遍历同一 Channel 下的多个 Credential（Key），跳过熔断中的端点，
 * 失败切下一个 Key，全部失败抛 ProviderException。</p>
 */
@Component
public class KeyFailoverInvoker {

    private static final Logger log = LoggerFactory.getLogger(KeyFailoverInvoker.class);

    private final CredentialResolver credentialResolver;
    private final UpstreamClientRegistry clientRegistry;
    private final ResilientClientFactory resilientClientFactory;
    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final MeterRegistry meterRegistry;

    public KeyFailoverInvoker(CredentialResolver credentialResolver,
                               UpstreamClientRegistry clientRegistry,
                               ResilientClientFactory resilientClientFactory,
                               ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                               MeterRegistry meterRegistry) {
        this.credentialResolver = credentialResolver;
        this.clientRegistry = clientRegistry;
        this.resilientClientFactory = resilientClientFactory;
        this.circuitBreakerManager = circuitBreakerManager;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 非流式调用 — 带 Key 级故障转移
     */
    public ProtocolResponse invoke(RoutingContext ctx, ProtocolRequest request) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        ProviderException lastException = null;

        for (ChannelCredential cred : credentials) {
            if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
                log.debug("端点 {} 熔断中，跳过 Key {}", ctx.channelEndpointId(), cred.getId());
                continue;
            }

            UpstreamClient client = buildClient(ctx, cred);

            try {
                return client.chat(request);
            } catch (ProviderException e) {
                lastException = e;
                meterRegistry.counter("gateway.failover.triggered",
                        "provider", provider,
                        "from_key", String.valueOf(cred.getId()),
                        "error_type", e.getErrorType().name()).increment();
                log.warn("Key {} 失败: {} {}, 尝试下一个 Key", cred.getId(), e.getErrorType(), e.getMessage());
            }
        }

        meterRegistry.counter("gateway.failover.exhausted",
                "provider", provider,
                "channel_id", String.valueOf(ctx.channelId())).increment();
        throw new ProviderException(
                lastException != null ? lastException.getErrorType() : ProviderErrorType.UPSTREAM_ERROR,
                "所有 Key 均失败: " + (lastException != null ? lastException.getMessage() : "无可用 Key"),
                null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }

    /**
     * 流式调用 — 调用前遍历 Key 检查熔断，传输开始后不切换
     */
    public void invokeStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();

        for (ChannelCredential cred : credentials) {
            if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
                log.debug("端点 {} 熔断中，跳过 Key {}", ctx.channelEndpointId(), cred.getId());
                continue;
            }

            UpstreamClient client = buildClient(ctx, cred);
            try {
                client.chatStream(request, callback);
                return;
            } catch (ProviderException e) {
                log.warn("Key {} 流式启动失败: {} {}, 尝试下一个 Key",
                        cred.getId(), e.getErrorType(), e.getMessage());
                meterRegistry.counter("gateway.failover.triggered",
                        "provider", provider,
                        "from_key", String.valueOf(cred.getId()),
                        "error_type", e.getErrorType().name()).increment();
            }
        }

        meterRegistry.counter("gateway.failover.exhausted",
                "provider", provider,
                "channel_id", String.valueOf(ctx.channelId())).increment();
        throw new ProviderException(
                ProviderErrorType.UPSTREAM_ERROR,
                "流式调用：所有 Key 均失败",
                null, request.getModel(), provider, ctx.channelEndpointId(), null);
    }

    private UpstreamClient buildClient(RoutingContext ctx, ChannelCredential cred) {
        UpstreamClient rawClient = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                cred.getApiKeyPlain(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        return resilientClientFactory.wrap(rawClient, ctx.channelEndpointId());
    }
}
