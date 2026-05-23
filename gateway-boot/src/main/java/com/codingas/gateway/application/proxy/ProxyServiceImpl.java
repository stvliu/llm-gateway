package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGateway;
import com.codingas.gateway.domain.proxy.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.protocol.OpenAIChatRequest;
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
 * <p>通过 ProtocolGatewayFactory 按协议名称创建绑定配置的 Gateway 实例分发请求。</p>
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
    public LLMResponse proxy(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy) {
        RoutingContext context = channelRoutingService.resolve(
                authResult, request.getModel(), request.getProtocol());

        log.info("Proxy request routed: model={}, productId={}, protocol={}, endpoint={}",
                request.getModel(), context.getProductId(), context.getProtocol(), context.getEndpoint());

        ProtocolGateway gateway = protocolGatewayFactory.create(
                context.getProtocol(), context.getEndpoint(), context.getProviderApiKey(), 60);

        // TODO: Task 10 将彻底重构此处，用 ProtocolRequest 替代 LLMRequest
        ProtocolRequest protocolRequest = convertToProtocolRequest(request, context.getProtocol());
        ProtocolResponse response = gateway.chat(protocolRequest);

        // 临时转换回 LLMResponse（Task 10 后将直接返回 ProtocolResponse）
        return convertToLLMResponse(response);
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

        ProtocolGateway gateway = protocolGatewayFactory.create(
                context.getProtocol(), context.getEndpoint(), context.getProviderApiKey(), 60);

        ProtocolRequest protocolRequest = convertToProtocolRequest(request, context.getProtocol());

        gateway.chatStream(protocolRequest, new StreamCallback() {
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

        ProtocolGateway gateway = protocolGatewayFactory.create(
                context.getProtocol(), context.getEndpoint(), context.getProviderApiKey(), 60);

        ProtocolRequest protocolRequest = convertToProtocolRequest(request, context.getProtocol());

        gateway.chatStream(protocolRequest, new StreamCallback() {
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

    /**
     * 临时将 LLMRequest 转换为协议 DTO（Task 10 后由 Controller 直接传协议 DTO）
     */
    private ProtocolRequest convertToProtocolRequest(LLMRequest request, String protocol) {
        if ("anthropic".equals(protocol)) {
            var messages = request.getMessages().stream()
                .map(msg -> com.codingas.gateway.domain.proxy.protocol.AnthropicMessagesRequest.Message.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build())
                .toList();
            return com.codingas.gateway.domain.proxy.protocol.AnthropicMessagesRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : 1024)
                .temperature(request.getTemperature())
                .stream(request.isStream())
                .build();
        } else {
            var messages = request.getMessages().stream()
                .map(msg -> OpenAIChatRequest.Message.builder()
                    .role(msg.getRole())
                    .content(msg.getContent())
                    .build())
                .toList();
            return OpenAIChatRequest.builder()
                .model(request.getModel())
                .messages(messages)
                .maxTokens(request.getMaxTokens())
                .temperature(request.getTemperature())
                .stream(request.isStream())
                .build();
        }
    }

    /**
     * 临时将 ProtocolResponse 转换回 LLMResponse（Task 10 后删除）
     */
    private LLMResponse convertToLLMResponse(ProtocolResponse response) {
        return LLMResponse.builder()
                .provider(response.getModel())
                .model(response.getModel())
                .finishReason(response.getFinishReason())
                .stream(false)
                .build();
    }
}