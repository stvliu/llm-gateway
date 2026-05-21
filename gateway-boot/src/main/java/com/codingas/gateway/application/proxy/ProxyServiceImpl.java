package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayRegistry;
import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.security.service.UserAuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 代理服务实现
 *
 * <p>通过 ProtocolGateway 按协议名称分发请求，而非按供应商类型。</p>
 */
@Service
public class ProxyServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private final ChannelRoutingService channelRoutingService;
    private final ProtocolGatewayRegistry protocolGatewayRegistry;

    public ProxyServiceImpl(ChannelRoutingService channelRoutingService,
                            ProtocolGatewayRegistry protocolGatewayRegistry) {
        this.channelRoutingService = channelRoutingService;
        this.protocolGatewayRegistry = protocolGatewayRegistry;
    }

    /**
     * 非流式聊天
     */
    public LLMResponse chat(UserAuthResult authResult, LLMRequest request) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Chat request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayRegistry.getGateway(context.getProtocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No protocol gateway found for: " + context.getProtocol()));

        return gateway.chat(request, context.getEndpoint(), context.getProviderApiKey(), 60);
    }

    /**
     * 流式聊天
     */
    public void chatStream(UserAuthResult authResult, LLMRequest request, StreamCallback callback) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Stream request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayRegistry.getGateway(context.getProtocol())
                .orElseThrow(() -> new IllegalStateException(
                        "No protocol gateway found for: " + context.getProtocol()));

        gateway.chatStream(request, context.getEndpoint(), context.getProviderApiKey(), 60, callback);
    }
}