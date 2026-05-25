package com.codingas.gateway.application.proxy;

import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.supply.gateway.ProtocolGateway;
import com.codingas.gateway.domain.supply.gateway.ProtocolGatewayFactory;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 代理服务实现
 *
 * <p>通过 ProtocolGatewayFactory 创建绑定 Provider 配置的 Gateway 实例分发请求，
 * 跨协议场景通过 ProtocolConverter 做请求/响应转换。</p>
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    private static final Logger log = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private final SupplyRoutingService supplyRoutingService;
    private final ProtocolGatewayFactory protocolGatewayFactory;
    private final ProtocolConverter protocolConverter;

    public ProxyServiceImpl(SupplyRoutingService supplyRoutingService,
                            ProtocolGatewayFactory protocolGatewayFactory,
                            ProtocolConverter protocolConverter) {
        this.supplyRoutingService = supplyRoutingService;
        this.protocolGatewayFactory = protocolGatewayFactory;
        this.protocolConverter = protocolConverter;
    }

    @Override
    public ProtocolResponse proxy(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        RoutingContext context = supplyRoutingService.resolve(
                identity, request.getModel(), request.getProtocol());

        log.info("Proxy request routed: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}",
                request.getModel(), context.channelId(), context.upstreamProtocol(), context.endpointUrl());

        String protocolName = context.upstreamProtocol() != null ? context.upstreamProtocol().name().toLowerCase() : "openai";
        int timeoutSeconds = context.timeout() != null ? context.timeout() : 60;
        ProtocolGateway gateway = protocolGatewayFactory.create(
                protocolName, context.endpointUrl(), context.providerApiKey(), timeoutSeconds);

        // 跨协议请求转换
        ProtocolRequest gatewayRequest = convertRequestIfNeeded(request, protocolName);
        ProtocolResponse response = gateway.chat(gatewayRequest);

        // 跨协议响应转换
        return convertResponseIfNeeded(response, protocolName, request.getProtocol());
    }

    @Override
    public void proxyStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        RoutingContext context = supplyRoutingService.resolve(
                identity, request.getModel(), request.getProtocol());

        log.info("Stream request routed: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}",
                request.getModel(), context.channelId(), context.upstreamProtocol(), context.endpointUrl());

        String protocolName = context.upstreamProtocol() != null ? context.upstreamProtocol().name().toLowerCase() : "openai";
        int timeoutSeconds = context.timeout() != null ? context.timeout() : 60;
        ProtocolGateway gateway = protocolGatewayFactory.create(
                protocolName, context.endpointUrl(), context.providerApiKey(), timeoutSeconds);

        // 跨协议请求转换
        ProtocolRequest gatewayRequest = convertRequestIfNeeded(request, protocolName);
        boolean needsConversion = !request.getProtocol().equals(protocolName);

        gateway.chatStream(gatewayRequest, new StreamCallback() {
            @Override
            public void onChunk(String data) {
                if (needsConversion) {
                    StreamChunkResult result = protocolConverter.convertStreamChunk(
                            data, protocolName, request.getProtocol());
                    if (result != null) {
                        // 跨协议场景：组装完整 SSE 行（含 event 类型）
                        if (result.eventType() != null) {
                            onChunk.accept("event: " + result.eventType() + "\ndata: " + result.data() + "\n\n");
                        } else {
                            onChunk.accept("data: " + result.data() + "\n\n");
                        }
                    }
                } else {
                    onChunk.accept(data);
                }
            }

            @Override
            public void onComplete() {
                if (needsConversion) {
                    StreamChunkResult doneResult = protocolConverter.convertStreamDone(
                            protocolName, request.getProtocol());
                    if (doneResult != null) {
                        if (doneResult.eventType() != null) {
                            onChunk.accept("event: " + doneResult.eventType() + "\ndata: " + doneResult.data() + "\n\n");
                        } else {
                            onChunk.accept("data: " + doneResult.data() + "\n\n");
                        }
                    }
                }
                onComplete.run();
            }

            @Override
            public void onError(Throwable error) {
                onError.accept(error);
            }
        });
    }

    /**
     * 跨协议请求转换
     */
    private ProtocolRequest convertRequestIfNeeded(ProtocolRequest request, String targetProtocol) {
        if (request.getProtocol().equals(targetProtocol)) {
            return request;
        }
        if (request instanceof OpenAIChatRequest openai && "anthropic".equals(targetProtocol)) {
            return protocolConverter.toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && "openai".equals(targetProtocol)) {
            return protocolConverter.toOpenAI(anthropic);
        }
        throw new IllegalArgumentException(
                "不支持的跨协议转换: " + request.getProtocol() + " → " + targetProtocol);
    }

    /**
     * 跨协议响应转换
     */
    private ProtocolResponse convertResponseIfNeeded(ProtocolResponse response,
                                                     String fromProtocol, String toProtocol) {
        if (fromProtocol.equals(toProtocol)) {
            return response;
        }
        if (response instanceof OpenAIChatResponse openai && "openai".equals(fromProtocol) && "anthropic".equals(toProtocol)) {
            return protocolConverter.toAnthropic(openai);
        }
        if (response instanceof AnthropicMessagesResponse anthropic && "anthropic".equals(fromProtocol) && "openai".equals(toProtocol)) {
            return protocolConverter.toOpenAI(anthropic);
        }
        log.warn("无法转换响应: {} → {}, 返回原始响应", fromProtocol, toProtocol);
        return response;
    }
}