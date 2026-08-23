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
package com.codingas.gateway.proxy.invoker;

import com.codingas.gateway.proxy.routing.CredentialResolver;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.provider.vendor.ProviderException;
import com.codingas.gateway.provider.upstream.ResilientClientFactory;
import com.codingas.gateway.provider.upstream.UpstreamClient;
import com.codingas.gateway.provider.upstream.UpstreamClientRegistry;
import com.codingas.gateway.provider.upstream.RoutingContext;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerManager;
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
     * 单 Key 调用（FAIL_FAST 策略用）— 只试第一个 Key，失败即抛，不换 Key
     *
     * <p>FAIL_FAST 策略下 {@link ChannelFailoverInvoker} 调用此方法：首个 Key 失败立即抛错，
     * 不试同渠道其他 Key（不跑 L0 Key 级转移），不换渠道（不跑 L1）。</p>
     *
     * <p>与 {@link #invoke} 的差异：invoke 遍历所有 Key 跳过熔断端点逐个试，
     * invokeSingleKey 只取第一个 Key，端点熔断或调用失败均直接抛（不试下一个 Key）。</p>
     *
     * @param ctx     路由上下文
     * @param request 协议请求
     * @return 上游响应
     * @throws ProviderException 首个 Key 调用失败、端点熔断中、或无可用 Key 时抛出
     */
    public ProtocolResponse invokeSingleKey(RoutingContext ctx, ProtocolRequest request) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        if (credentials.isEmpty()) {
            throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                    "无可用 Key", null, request.getModel(), provider, ctx.channelEndpointId(), null);
        }
        ChannelCredential cred = credentials.get(0);
        if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
            log.debug("端点 {} 熔断中，FAIL_FAST 不试 Key {}", ctx.channelEndpointId(), cred.getId());
            throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                    "端点熔断中: " + ctx.channelEndpointId(),
                    null, request.getModel(), provider, ctx.channelEndpointId(), null);
        }
        UpstreamClient client = buildClient(ctx, cred);
        try {
            return client.chat(request);
        } catch (ProviderException e) {
            meterRegistry.counter("gateway.failover.triggered",
                    "provider", provider,
                    "from_key", String.valueOf(cred.getId()),
                    "error_type", e.getErrorType().name()).increment();
            log.warn("Key {} 失败(FAIL_FAST): {} {}, 不换 Key 直接抛", cred.getId(), e.getErrorType(), e.getMessage());
            throw e;
        }
    }

    /**
     * 单 Key 流式调用（FAIL_FAST 策略用）— 只试第一个 Key，失败即抛，不换 Key
     *
     * <p>FAIL_FAST 策略下 {@link ChannelFailoverInvoker} 调用此方法：首个 Key 流式启动失败立即抛错，
     * 不试同渠道其他 Key。</p>
     *
     * @param ctx      路由上下文
     * @param request  协议请求
     * @param callback 流式回调
     * @throws ProviderException 首个 Key 流式启动失败、端点熔断中、或无可用 Key 时抛出
     */
    public void invokeSingleKeyStream(RoutingContext ctx, ProtocolRequest request, StreamCallback callback) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        if (credentials.isEmpty()) {
            throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                    "流式调用：无可用 Key", null, request.getModel(), provider, ctx.channelEndpointId(), null);
        }
        ChannelCredential cred = credentials.get(0);
        if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
            log.debug("端点 {} 熔断中，FAIL_FAST 流式不试 Key {}", ctx.channelEndpointId(), cred.getId());
            throw new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                    "流式调用：端点熔断中: " + ctx.channelEndpointId(),
                    null, request.getModel(), provider, ctx.channelEndpointId(), null);
        }
        UpstreamClient client = buildClient(ctx, cred);
        try {
            client.chatStream(request, callback);
        } catch (ProviderException e) {
            meterRegistry.counter("gateway.failover.triggered",
                    "provider", provider,
                    "from_key", String.valueOf(cred.getId()),
                    "error_type", e.getErrorType().name()).increment();
            log.warn("Key {} 流式启动失败(FAIL_FAST): {} {}, 不换 Key 直接抛",
                    cred.getId(), e.getErrorType(), e.getMessage());
            throw e;
        }
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
