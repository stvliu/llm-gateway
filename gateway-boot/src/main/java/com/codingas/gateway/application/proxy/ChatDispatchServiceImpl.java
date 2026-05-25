package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 聊天调度服务实现
 *
 * <p>七阶段调用链：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 */
@Service
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChatDispatchServiceImpl.class);

    private final RoutingResolver routingResolver;
    private final OutboundTuner outboundTuner;
    private final UpstreamClientRegistry clientRegistry;
    private final ProtocolConverter protocolConverter;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   OutboundTuner outboundTuner,
                                   UpstreamClientRegistry clientRegistry,
                                   ProtocolConverter protocolConverter) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.clientRegistry = clientRegistry;
        this.protocolConverter = protocolConverter;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        // 阶段 2：路由
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol);

        log.info("Dispatch request: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl());

        ProtocolRequest outboundReq = request;

        // 阶段 3：请求转换（仅跨协议时执行）
        if (ctx.needsProtocolAdaptation()) {
            outboundReq = convertRequest(request, ctx);
        }

        // 阶段 4：出站调谐
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        // 阶段 5：上游调用
        UpstreamClient client = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                ctx.providerApiKey(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        ProtocolResponse response = client.chat(outboundReq);

        // 阶段 6：响应转换（仅跨协议时执行）
        if (ctx.needsProtocolAdaptation()) {
            response = convertResponse(response, ctx, inboundProtocol);
        }

        // 阶段 7：后置处理（审计、计量 — 阶段 3 实现）
        return response;
    }

    @Override
    public void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                               StreamCallback callback) {
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol);

        log.info("Stream dispatch: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl());

        ProtocolRequest outboundReq = request;
        if (ctx.needsProtocolAdaptation()) {
            outboundReq = convertRequest(request, ctx);
        }
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        UpstreamClient client = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                ctx.providerApiKey(),
                ctx.timeout() != null ? ctx.timeout() : 60);

        if (ctx.needsProtocolAdaptation()) {
            String fromProtocol = ctx.upstreamProtocol().name().toLowerCase();
            String toProtocol = inboundProtocol.name().toLowerCase();
            client.chatStream(outboundReq, new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    StreamChunkResult result = protocolConverter.convertStreamChunk(data, fromProtocol, toProtocol);
                    if (result != null) {
                        if (result.eventType() != null) {
                            callback.onChunk("event: " + result.eventType() + "\ndata: " + result.data() + "\n\n");
                        } else {
                            callback.onChunk("data: " + result.data() + "\n\n");
                        }
                    }
                }

                @Override
                public void onComplete() {
                    StreamChunkResult doneResult = protocolConverter.convertStreamDone(fromProtocol, toProtocol);
                    if (doneResult != null) {
                        if (doneResult.eventType() != null) {
                            callback.onChunk("event: " + doneResult.eventType() + "\ndata: " + doneResult.data() + "\n\n");
                        } else {
                            callback.onChunk("data: " + doneResult.data() + "\n\n");
                        }
                    }
                    callback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    callback.onError(t);
                }
            });
        } else {
            client.chatStream(outboundReq, callback);
        }
    }

    private Protocol getInboundProtocol(ProtocolRequest request) {
        if (request instanceof OpenAIChatRequest) return Protocol.OPENAI;
        if (request instanceof AnthropicMessagesRequest) return Protocol.ANTHROPIC;
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private ProtocolRequest convertRequest(ProtocolRequest request, RoutingContext ctx) {
        if (request instanceof OpenAIChatRequest openai && ctx.upstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && ctx.upstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toOpenAI(anthropic);
        }
        return request;
    }

    private ProtocolResponse convertResponse(ProtocolResponse response, RoutingContext ctx, Protocol inboundProtocol) {
        if (response instanceof AnthropicMessagesResponse anthropic && ctx.upstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toOpenAI(anthropic);
        }
        if (response instanceof OpenAIChatResponse openai && ctx.upstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toAnthropic(openai);
        }
        log.warn("无法转换响应: {} → {}, 返回原始响应", ctx.upstreamProtocol(), inboundProtocol);
        return response;
    }
}