package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.protocol.ProtocolRequest;
import com.codingas.gateway.domain.proxy.protocol.ProtocolResponse;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 代理服务实现
 *
 * <p>通过 ProtocolGatewayFactory 创建绑定 Provider 配置的 Gateway 实例分发请求。</p>
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private final ChannelRoutingService channelRoutingService;
    private final ProtocolGatewayFactory protocolGatewayFactory;

    public ProxyServiceImpl(ChannelRoutingService channelRoutingService,
                            ProtocolGatewayFactory protocolGatewayFactory) {
        this.channelRoutingService = channelRoutingService;
        this.protocolGatewayFactory = protocolGatewayFactory;
    }

    @Override
    public ProtocolResponse proxy(ProtocolRequest request, UserAuthResult authResult, RoutingStrategy strategy) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Proxy request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayFactory.create(
                context.getProtocol(), context.getEndpoint(), context.getProviderApiKey(), 60);

        return gateway.chat(request);
    }

    @Override
    public void proxyStream(ProtocolRequest request, UserAuthResult authResult, RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Stream request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayFactory.create(
                context.getProtocol(), context.getEndpoint(), context.getProviderApiKey(), 60);

        gateway.chatStream(request, new StreamCallback() {
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