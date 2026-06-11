package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.invoker.DegradationInvoker;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
    private final ProtocolConverter protocolConverter;
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;
    private final DegradationInvoker degradationInvoker;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   OutboundTuner outboundTuner,
                                   ProtocolConverter protocolConverter,
                                   AuditGateway auditGateway,
                                   DomainEventPublisher eventPublisher,
                                   DegradationInvoker degradationInvoker) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.protocolConverter = protocolConverter;
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
        this.degradationInvoker = degradationInvoker;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        String traceId = UUID.randomUUID().toString();
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol, identity.userId(), identity.role(), strategy);

        log.info("Dispatch request: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, ctx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        try {
            ProtocolRequest outboundReq = request;

            // 阶段 3：请求转换（仅跨协议时执行）
            if (ctx.needsProtocolAdaptation()) {
                outboundReq = convertRequest(request, ctx);
            }

            // 阶段 4：出站调谐
            outboundReq = outboundTuner.tune(outboundReq, ctx);

            // 阶段 5：Invoker 链调用（熔断 + 重试 + Key 故障转移 + 降级）
            ProtocolResponse response = degradationInvoker.invoke(
                    ctx, outboundReq, inboundProtocol, identity.userId(), identity.role(), strategy);

            // 阶段 6：响应转换（仅跨协议时执行）
            if (ctx.needsProtocolAdaptation()) {
                response = convertResponse(response, ctx, inboundProtocol);
            }

            // 阶段 7：后置处理 — 审计终点 + Token 计量
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            callLog.setSuccess(true);
            publishTokenUsedEvent(response, identity, ctx, traceId);
            auditGateway.saveCallLog(callLog);

            return response;
        } catch (Exception e) {
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            callLog.setSuccess(false);
            callLog.setErrorMessage(e.getMessage());
            auditGateway.saveCallLog(callLog);
            throw e;
        }
    }

    @Override
    public void dispatchStream(ProtocolRequest request, Identity identity, RoutingStrategy strategy,
                               StreamCallback callback) {
        String traceId = UUID.randomUUID().toString();
        Protocol inboundProtocol = getInboundProtocol(request);
        RoutingContext ctx = routingResolver.resolve(request.getModel(), inboundProtocol, identity.userId(), identity.role(), strategy);

        log.info("Stream dispatch: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), ctx.channelId(), ctx.upstreamProtocol(), ctx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, ctx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        ProtocolRequest outboundReq = request;
        if (ctx.needsProtocolAdaptation()) {
            outboundReq = convertRequest(request, ctx);
        }
        outboundReq = outboundTuner.tune(outboundReq, ctx);

        StreamCallback auditingCallback = new StreamCallback() {
            @Override
            public void onChunk(String data) {
                callback.onChunk(data);
            }

            @Override
            public void onComplete() {
                callLog.setDurationMs(System.currentTimeMillis() - startTime);
                callLog.setSuccess(true);
                auditGateway.saveCallLog(callLog);
                callback.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                String errorJson;
                if (t instanceof com.codingas.gateway.domain.supply.exception.ProviderException pe) {
                    errorJson = com.codingas.gateway.infrastructure.upstream.SseErrorFormatter.format(pe);
                } else {
                    errorJson = "{\"error\":\"unknown_error\",\"retry_after\":0}";
                }
                callLog.setDurationMs(System.currentTimeMillis() - startTime);
                callLog.setSuccess(false);
                callLog.setErrorMessage(errorJson);
                auditGateway.saveCallLog(callLog);
                callback.onError(new RuntimeException(errorJson));
            }
        };

        if (ctx.needsProtocolAdaptation()) {
            // 跨协议：在 callback 中做协议转换
            String fromProtocol = ctx.upstreamProtocol().name().toLowerCase();
            String toProtocol = inboundProtocol.name().toLowerCase();
            StreamCallback convertingCallback = new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    StreamChunkResult result = protocolConverter.convertStreamChunk(data, fromProtocol, toProtocol);
                    if (result != null) {
                        auditingCallback.onChunk(result.data());
                    }
                }

                @Override
                public void onComplete() {
                    StreamChunkResult doneResult = protocolConverter.convertStreamDone(fromProtocol, toProtocol);
                    if (doneResult != null) {
                        auditingCallback.onChunk(doneResult.data());
                    }
                    auditingCallback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    auditingCallback.onError(t);
                }
            };
            degradationInvoker.invokeStream(ctx, outboundReq, convertingCallback,
                    inboundProtocol, identity.userId(), identity.role(), strategy);
        } else {
            // 非跨协议：注入协议对应的结束标记
            StreamCallback protocolCallback = new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    auditingCallback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    if (inboundProtocol == Protocol.OPENAI) {
                        auditingCallback.onChunk("[DONE]");
                    }
                    auditingCallback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    auditingCallback.onError(t);
                }
            };
            degradationInvoker.invokeStream(ctx, outboundReq, protocolCallback,
                    inboundProtocol, identity.userId(), identity.role(), strategy);
        }
    }

    /**
     * 创建调用日志记录
     */
    private CallLog createCallLog(Identity identity, ProtocolRequest request, RoutingContext ctx, Protocol inboundProtocol, String traceId) {
        CallLog callLog = new CallLog();
        callLog.setTraceId(traceId);
        callLog.setUserId(identity.userId());
        callLog.setModel(request.getModel());
        callLog.setChannelId(ctx.channelId());
        callLog.setChannelEndpointId(ctx.channelEndpointId());
        callLog.setInboundProtocol(inboundProtocol.name());
        callLog.setUpstreamProtocol(ctx.upstreamProtocol().name());
        callLog.setCalledAt(java.time.Instant.now());
        return callLog;
    }

    /**
     * 发布 Token 使用事件
     */
    private void publishTokenUsedEvent(ProtocolResponse response, Identity identity, RoutingContext ctx, String traceId) {
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        if (response instanceof OpenAIChatResponse openai && openai.getUsage() != null) {
            int inputTokens = openai.getUsage().getPromptTokens() != null ? openai.getUsage().getPromptTokens() : 0;
            int outputTokens = openai.getUsage().getCompletionTokens() != null ? openai.getUsage().getCompletionTokens() : 0;
            eventPublisher.publish(TokenUsedEvent.builder()
                    .userId(identity.userId())
                    .apiKeyId(identity.credentialId())
                    .model(openai.getModel())
                    .provider(provider)
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .traceId(traceId)
                    .build());
        } else if (response instanceof AnthropicMessagesResponse anthropic && anthropic.getUsage() != null) {
            int inputTokens = anthropic.getUsage().getInputTokens() != null ? anthropic.getUsage().getInputTokens() : 0;
            int outputTokens = anthropic.getUsage().getOutputTokens() != null ? anthropic.getUsage().getOutputTokens() : 0;
            eventPublisher.publish(TokenUsedEvent.builder()
                    .userId(identity.userId())
                    .apiKeyId(identity.credentialId())
                    .model(anthropic.getModel())
                    .provider(provider)
                    .promptTokens(inputTokens)
                    .completionTokens(outputTokens)
                    .traceId(traceId)
                    .build());
        }
    }

    private Protocol getInboundProtocol(ProtocolRequest request) {
        String protocol = request.getProtocol();
        if ("openai".equals(protocol)) return Protocol.OPENAI;
        if ("anthropic".equals(protocol)) return Protocol.ANTHROPIC;
        throw new IllegalArgumentException("不支持的协议类型: " + protocol);
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
        log.warn("无法转换响应: {} → {},返回原始响应", ctx.upstreamProtocol(), inboundProtocol);
        return response;
    }

    }