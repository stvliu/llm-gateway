package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 代理服务实现
 *
 * <p>通过 ProtocolGateway 按协议名称分发请求，而非按供应商类型。</p>
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private final ChannelRoutingService channelRoutingService;
    private final ProtocolGatewayRegistry protocolGatewayRegistry;

    public ProxyServiceImpl(ChannelRoutingService channelRoutingService,
                            ProtocolGatewayRegistry protocolGatewayRegistry) {
        this.channelRoutingService = channelRoutingService;
        this.protocolGatewayRegistry = protocolGatewayRegistry;
    }

    @Override
    public LLMResponse proxy(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Proxy request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayRegistry.getGateway(context.getProtocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No protocol gateway found for: " + context.getProtocol()));

        return gateway.chat(request, context.getEndpoint(), context.getProviderApiKey(), 60);
    }

    @Override
    public LLMResponse proxy(LLMRequest request, RoutingStrategy strategy) {
        throw new UnsupportedOperationException("请使用 proxy(request, authResult, strategy)");
    }

    @Override
    public void proxyStream(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy,
                            Consumer<String> onChunk) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Stream request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayRegistry.getGateway(context.getProtocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No protocol gateway found for: " + context.getProtocol()));

        gateway.chatStream(request, context.getEndpoint(), context.getProviderApiKey(), 60,
                new StreamCallback() {
                    @Override
                    public void onChunk(String data) {
                        onChunk.accept(data);
                    }

                    @Override
                    public void onComplete() {}

                    @Override
                    public void onError(Throwable error) {
                        log.error("Stream error: {}", error.getMessage());
                    }
                });
    }

    @Override
    public void proxyStream(LLMRequest request, RoutingStrategy strategy, Consumer<String> onChunk) {
        throw new UnsupportedOperationException("请使用 proxyStream(request, authResult, strategy, onChunk, onComplete, onError)");
    }

    @Override
    public void proxyStream(LLMRequest request, RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        throw new UnsupportedOperationException("请使用 proxyStream(request, authResult, strategy, onChunk, onComplete, onError)");
    }

    @Override
    public void proxyStream(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Stream request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayRegistry.getGateway(context.getProtocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No protocol gateway found for: " + context.getProtocol()));

        gateway.chatStream(request, context.getEndpoint(), context.getProviderApiKey(), 60,
                new StreamCallback() {
                    @Override
                    public void onChunk(String data) {
                        onChunk.accept(data);
                    }

                    @Override
                    public void onComplete() {
                        onComplete.run();
                    }

                    @Override
                    public void onError(Throwable error) {
                        onError.accept(error);
                    }
                });
    }
}
