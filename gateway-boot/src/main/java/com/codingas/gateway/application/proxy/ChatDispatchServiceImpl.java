package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.degradation.DegradationService;
import com.codingas.gateway.application.proxy.routing.CredentialResolver;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.entity.ChannelCredential;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ResilientClientFactory;
import com.codingas.gateway.domain.supply.gateway.UpstreamClient;
import com.codingas.gateway.domain.supply.gateway.UpstreamClientRegistry;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import com.codingas.gateway.infrastructure.upstream.SseErrorFormatter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
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
    private final UpstreamClientRegistry clientRegistry;
    private final ProtocolConverter protocolConverter;
    private final ResilientClientFactory resilientClientFactory;
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;
    private final CredentialResolver credentialResolver;
    private final ChannelEndpointCircuitBreakerManager circuitBreakerManager;
    private final DegradationService degradationService;
    private final MeterRegistry meterRegistry;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   OutboundTuner outboundTuner,
                                   UpstreamClientRegistry clientRegistry,
                                   ProtocolConverter protocolConverter,
                                   ResilientClientFactory resilientClientFactory,
                                   AuditGateway auditGateway,
                                   DomainEventPublisher eventPublisher,
                                   CredentialResolver credentialResolver,
                                   ChannelEndpointCircuitBreakerManager circuitBreakerManager,
                                   DegradationService degradationService,
                                   MeterRegistry meterRegistry) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.clientRegistry = clientRegistry;
        this.protocolConverter = protocolConverter;
        this.resilientClientFactory = resilientClientFactory;
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
        this.credentialResolver = credentialResolver;
        this.circuitBreakerManager = circuitBreakerManager;
        this.degradationService = degradationService;
        this.meterRegistry = meterRegistry;
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

            // 阶段 5：上游调用（带韧性保护 + Key 级故障转移）
            ProtocolResponse response = callWithKeyFailover(ctx, outboundReq, traceId);

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

            // 尝试降级：ProviderException 时切换到备选模型
            if (e instanceof ProviderException pe) {
                String fallbackModel = degradationService.degrade(request.getModel(), pe.getErrorType());
                if (fallbackModel != null) {
                    log.info("模型 {} 降级为 {}，重新调度", request.getModel(), fallbackModel);
                    request.setModel(fallbackModel);
                    return dispatch(request, identity, strategy);
                }
            }
            throw e;
        }
    }

    /**
     * 带 Key 级故障转移的上游调用
     *
     * <p>遍历同一 Channel 下的多个 Key，跳过熔断中的 Key，重试耗尽后切换到下一个 Key。</p>
     */
    private ProtocolResponse callWithKeyFailover(RoutingContext ctx, ProtocolRequest outboundReq, String traceId) {
        List<ChannelCredential> credentials = credentialResolver.resolveAll(ctx.channelId());
        String provider = ctx.upstreamProtocol().name().toLowerCase();
        ProviderException lastException = null;

        for (ChannelCredential cred : credentials) {
            if (!circuitBreakerManager.isAvailable(ctx.channelEndpointId())) {
                log.debug("端点 {} 熔断中，跳过 Key {}", ctx.channelEndpointId(), cred.getId());
                continue;
            }

            UpstreamClient rawClient = clientRegistry.getClient(
                    ctx.upstreamProtocol().name().toLowerCase(),
                    ctx.endpointUrl(),
                    cred.getApiKeyPlain(),
                    ctx.timeout() != null ? ctx.timeout() : 60);
            UpstreamClient client = resilientClientFactory.wrap(rawClient, ctx.channelEndpointId());

            try {
                return client.chat(outboundReq);
            } catch (ProviderException e) {
                lastException = e;
                meterRegistry.counter("gateway.failover.triggered",
                        "provider", provider,
                        "from_key", String.valueOf(cred.getId()),
                        "error_type", e.getErrorType().name()).increment();
                log.warn("Key {} 失败: {} {}, 尝试下一个 Key", cred.getId(), e.getErrorType(), e.getMessage());
            }
        }

        // 所有 Key 失败，注入上下文后抛出
        meterRegistry.counter("gateway.failover.exhausted",
                "provider", provider,
                "channel_id", String.valueOf(ctx.channelId())).increment();
        throw new ProviderException(
                lastException != null ? lastException.getErrorType() : ProviderErrorType.UPSTREAM_ERROR,
                "所有 Key 均失败: " + (lastException != null ? lastException.getMessage() : "无可用 Key"),
                traceId, outboundReq.getModel(), provider, ctx.channelEndpointId(), null);
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

        UpstreamClient client = getResilientClient(ctx);

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
                if (t instanceof ProviderException pe) {
                    errorJson = SseErrorFormatter.format(pe);
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
            dispatchStreamCrossProtocol(client, outboundReq, ctx, inboundProtocol, auditingCallback);
        } else {
            // 非跨协议：包装 callback，注入协议对应的结束标记
            StreamCallback protocolCallback = new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    auditingCallback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    // OpenAI 入站注入 [DONE] 标记；Anthropic 无需标记，流关闭即结束
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
            client.chatStream(outboundReq, protocolCallback);
        }
    }

    /**
     * 获取带韧性保护的上游客户端
     */
    private UpstreamClient getResilientClient(RoutingContext ctx) {
        UpstreamClient rawClient = clientRegistry.getClient(
                ctx.upstreamProtocol().name().toLowerCase(),
                ctx.endpointUrl(),
                ctx.providerApiKey(),
                ctx.timeout() != null ? ctx.timeout() : 60);
        return resilientClientFactory.wrap(rawClient, ctx.channelEndpointId());
    }

    /**
     * 跨协议流式调度：转换 chunk + done 事件
     */
    private void dispatchStreamCrossProtocol(UpstreamClient client, ProtocolRequest outboundReq,
                                              RoutingContext ctx, Protocol inboundProtocol,
                                              StreamCallback callback) {
        String fromProtocol = ctx.upstreamProtocol().name().toLowerCase();
        String toProtocol = inboundProtocol.name().toLowerCase();

        client.chatStream(outboundReq, new StreamCallback() {
            @Override
            public void onChunk(String data) {
                StreamChunkResult result = protocolConverter.convertStreamChunk(data, fromProtocol, toProtocol);
                if (result != null) {
                    // 直接传递 data，由 SseStreamHelper.writeChunk 统一加 "data: " 前缀
                    // eventType 已编码在 JSON payload 的 "type" 字段中
                    callback.onChunk(result.data());
                }
            }

            @Override
            public void onComplete() {
                StreamChunkResult doneResult = protocolConverter.convertStreamDone(fromProtocol, toProtocol);
                if (doneResult != null) {
                    // 完成标记（如 "[DONE]"）直接传递，由 writeChunk 格式化
                    callback.onChunk(doneResult.data());
                }
                callback.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                callback.onError(t);
            }
        });
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