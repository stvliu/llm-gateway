package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.application.proxy.invoker.L2DegradationRequiredException;
import com.codingas.gateway.application.proxy.routing.RoutingResolver;
import com.codingas.gateway.application.resilience.ResilienceResolver;
import com.codingas.gateway.domain.audit.entity.CallLog;
import com.codingas.gateway.domain.audit.gateway.AuditGateway;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
import com.codingas.gateway.domain.protocol.contract.*;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import com.codingas.gateway.domain.iam.valueobject.Identity;
import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 聊天调度服务实现
 *
 * <p>七阶段调用链：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 *
 * <p><b>Task 3.6 适配</b>：DegradationInvoker 退场后，改用 {@link ChannelFailoverInvoker}。
 * 调用链改为：先 {@link RoutingResolver#resolveCandidates} 取候选列表，再调
 * {@link ChannelFailoverInvoker#invoke}（L1 候选内逐个试 + L2 模型降级）。L2 降级时
 * ChannelFailoverInvoker 抛 {@link L2DegradationRequiredException}，本类捕获后用 fallback
 * 模型重新 resolveCandidates 并重试（防递归：已降级模型集合去重 + 深度上限兜底）。</p>
 */
@Service
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChatDispatchServiceImpl.class);

    /** L2 降级最大深度（含原始调用，即最多降级 maxDegradationDepth 次） */
    private static final int MAX_DEGRADATION_DEPTH = 5;

    private final RoutingResolver routingResolver;
    private final OutboundTuner outboundTuner;
    private final ProtocolConverter protocolConverter;
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;
    private final ChannelFailoverInvoker channelFailoverInvoker;
    /** 容灾画像解析器（Task 4.9：贯穿 Invoker 链做 L2 门禁） */
    private final ResilienceResolver resilienceResolver;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   OutboundTuner outboundTuner,
                                   ProtocolConverter protocolConverter,
                                   AuditGateway auditGateway,
                                   DomainEventPublisher eventPublisher,
                                   ChannelFailoverInvoker channelFailoverInvoker,
                                   ResilienceResolver resilienceResolver) {
        this.routingResolver = routingResolver;
        this.outboundTuner = outboundTuner;
        this.protocolConverter = protocolConverter;
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
        this.channelFailoverInvoker = channelFailoverInvoker;
        this.resilienceResolver = resilienceResolver;
    }

    @Override
    public ProtocolResponse dispatch(ProtocolRequest request, Identity identity, RoutingStrategy strategy) {
        String traceId = UUID.randomUUID().toString();
        Protocol inboundProtocol = getInboundProtocol(request);
        // 阶段 2：路由解析 — 取候选列表（L1 候选内逐个试），主候选为首项
        List<RoutingContext> candidates = routingResolver.resolveCandidates(
                request.getModel(), inboundProtocol, identity.applicationId(), identity.userId(), identity.role(), strategy);
        RoutingContext primaryCtx = candidates.get(0);

        log.info("Dispatch request: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), primaryCtx.channelId(), primaryCtx.upstreamProtocol(), primaryCtx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, primaryCtx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        try {
            ProtocolRequest outboundReq = request;

            // 阶段 3：请求转换（仅跨协议时执行）
            if (primaryCtx.needsProtocolAdaptation()) {
                outboundReq = convertRequest(request, primaryCtx);
            }

            // 阶段 4：出站调谐
            outboundReq = outboundTuner.tune(outboundReq, primaryCtx);

            // 阶段 5：Invoker 链调用（熔断 + 重试 + Key 故障转移 + L1 候选转移 + L2 模型降级重路由）
            // 解析容灾画像贯穿 Invoker 链（L2 门禁），fail-open：解析异常降级 null profile
            ResilienceProfile profile = resolveProfileSafely(identity.applicationId());
            ProtocolResponse response = invokeWithL2Failover(primaryCtx, candidates, outboundReq,
                    inboundProtocol, identity, strategy, profile);

            // 阶段 6：响应转换（仅跨协议时执行）
            if (primaryCtx.needsProtocolAdaptation()) {
                response = convertResponse(response, primaryCtx, inboundProtocol);
            }

            // 阶段 7：后置处理 — 审计终点 + Token 计量
            callLog.setDurationMs(System.currentTimeMillis() - startTime);
            callLog.setSuccess(true);
            publishTokenUsedEvent(response, identity, primaryCtx, traceId);
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
        // 阶段 2：路由解析 — 取候选列表，主候选为首项
        List<RoutingContext> candidates = routingResolver.resolveCandidates(
                request.getModel(), inboundProtocol, identity.applicationId(), identity.userId(), identity.role(), strategy);
        RoutingContext primaryCtx = candidates.get(0);

        log.info("Stream dispatch: model={}, channelId={}, upstreamProtocol={}, endpointUrl={}, traceId={}",
                request.getModel(), primaryCtx.channelId(), primaryCtx.upstreamProtocol(), primaryCtx.endpointUrl(), traceId);

        CallLog callLog = createCallLog(identity, request, primaryCtx, inboundProtocol, traceId);
        long startTime = System.currentTimeMillis();

        ProtocolRequest outboundReq = request;
        if (primaryCtx.needsProtocolAdaptation()) {
            outboundReq = convertRequest(request, primaryCtx);
        }
        outboundReq = outboundTuner.tune(outboundReq, primaryCtx);

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

        // 根据是否跨协议选择 delegate callback（L2 降级重路由时复用，流未建立前 callback 未触发）
        StreamCallback delegateCallback;
        if (primaryCtx.needsProtocolAdaptation()) {
            // 跨协议：在 callback 中做协议转换
            String fromProtocol = primaryCtx.upstreamProtocol().name().toLowerCase();
            String toProtocol = inboundProtocol.name().toLowerCase();
            delegateCallback = new StreamCallback() {
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
        } else {
            // 非跨协议：注入协议对应的结束标记
            delegateCallback = new StreamCallback() {
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
        }

        // 阶段 5：Invoker 链调用（L1 候选转移 + L2 模型降级重路由，首字节前启动失败可重路由）
        // 解析容灾画像贯穿 Invoker 链（L2 门禁），fail-open：解析异常降级 null profile
        ResilienceProfile profile = resolveProfileSafely(identity.applicationId());
        invokeStreamWithL2Failover(primaryCtx, candidates, outboundReq, delegateCallback,
                inboundProtocol, identity, strategy, profile);
    }

    /**
     * 非流式 L2 降级重路由循环
     *
     * <p>调用 {@link ChannelFailoverInvoker#invoke}，捕获 {@link L2DegradationRequiredException} 后
     * 用 fallback 模型重新 {@link RoutingResolver#resolveCandidates} 并重试。</p>
     *
     * <p><b>防递归</b>：已降级模型集合 {@code degradedModels} 去重（fallback 已降级过则抛原始异常）+
     * 深度上限 {@link #MAX_DEGRADATION_DEPTH} 兜底（超限抛原始异常），双保险防止降级链成环或无限延伸。
     * 原始上游失败作为 L2 信号的 cause 保留，防递归/深度超限时通过 {@link #unwrapL2Cause} 解包抛出，
     * 保留原始失败上下文。</p>
     *
     * @param primaryCtx      主路由上下文（候选首项）
     * @param candidates      候选列表
     * @param request         协议请求（降级时 setModel 修改为 fallback）
     * @param inboundProtocol 入站协议
     * @param identity        调用方身份（含 applicationId/userId/role，重路由透传）
     * @param strategy        路由策略
     * @return 上游响应
     */
    private ProtocolResponse invokeWithL2Failover(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                                  ProtocolRequest request, Protocol inboundProtocol,
                                                  Identity identity, RoutingStrategy strategy,
                                                  ResilienceProfile profile) {
        Set<String> degradedModels = new HashSet<>();
        degradedModels.add(request.getModel());
        List<RoutingContext> currentCandidates = candidates;
        RoutingContext currentPrimaryCtx = primaryCtx;
        L2DegradationRequiredException lastL2Signal = null;
        // 重路由深度上限：画像 degradationMaxDepth 优先，无画像回退默认 MAX_DEGRADATION_DEPTH
        int maxDepth = resolveMaxDepth(profile);

        for (int depth = 0; depth <= maxDepth; depth++) {
            try {
                return channelFailoverInvoker.invoke(currentPrimaryCtx, currentCandidates, request,
                        inboundProtocol, identity.applicationId(), profile);
            } catch (L2DegradationRequiredException e) {
                lastL2Signal = e;
                String fallbackModel = e.getFallbackModel();
                // 防递归：fallback 已降级过，抛原始上游异常
                if (degradedModels.contains(fallbackModel)) {
                    log.warn("L2 降级防递归：fallback 模型 {} 已降级过，抛出原始上游异常", fallbackModel);
                    throw unwrapL2Cause(e);
                }
                degradedModels.add(fallbackModel);
                log.info("L2 降级：模型 {} → {}，重新路由 (depth={})", request.getModel(), fallbackModel, depth);
                request.setModel(fallbackModel);
                currentCandidates = routingResolver.resolveCandidates(fallbackModel, inboundProtocol,
                        identity.applicationId(), identity.userId(), identity.role(), strategy);
                // fallback 无可用候选：抛原始上游异常
                if (currentCandidates == null || currentCandidates.isEmpty()) {
                    log.warn("L2 降级：fallback 模型 {} 无可用候选，抛出原始上游异常", fallbackModel);
                    throw unwrapL2Cause(e);
                }
                currentPrimaryCtx = currentCandidates.get(0);
            }
        }
        log.warn("L2 降级深度超限 (max={})，抛出原始上游异常", maxDepth);
        throw unwrapL2Cause(lastL2Signal);
    }

    /**
     * 流式 L2 降级重路由循环
     *
     * <p>语义同 {@link #invokeWithL2Failover}，仅回调签名不同。L2 重路由仅在"所有候选首字节前
     * 启动失败"时触发（ChannelFailoverInvoker 首字节后失败不换渠道也不进 L2），此时流未建立、
     * callback 未触发，可安全复用 callback 重新调用。</p>
     *
     * @param primaryCtx      主路由上下文
     * @param candidates      候选列表
     * @param request         协议请求
     * @param callback        流式回调（L2 重路由时复用）
     * @param inboundProtocol 入站协议
     * @param identity        调用方身份
     * @param strategy        路由策略
     * @param profile         容灾画像（L2 门禁贯穿 Invoker 链）
     */
    private void invokeStreamWithL2Failover(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                            ProtocolRequest request, StreamCallback callback,
                                            Protocol inboundProtocol, Identity identity, RoutingStrategy strategy,
                                            ResilienceProfile profile) {
        Set<String> degradedModels = new HashSet<>();
        degradedModels.add(request.getModel());
        List<RoutingContext> currentCandidates = candidates;
        RoutingContext currentPrimaryCtx = primaryCtx;
        L2DegradationRequiredException lastL2Signal = null;
        // 重路由深度上限：画像 degradationMaxDepth 优先，无画像回退默认 MAX_DEGRADATION_DEPTH
        int maxDepth = resolveMaxDepth(profile);

        for (int depth = 0; depth <= maxDepth; depth++) {
            try {
                channelFailoverInvoker.invokeStream(currentPrimaryCtx, currentCandidates, request,
                        inboundProtocol, identity.applicationId(), profile, callback);
                return;
            } catch (L2DegradationRequiredException e) {
                lastL2Signal = e;
                String fallbackModel = e.getFallbackModel();
                if (degradedModels.contains(fallbackModel)) {
                    log.warn("流式 L2 降级防递归：fallback 模型 {} 已降级过，抛出原始上游异常", fallbackModel);
                    throw unwrapL2Cause(e);
                }
                degradedModels.add(fallbackModel);
                log.info("流式 L2 降级：模型 {} → {}，重新路由 (depth={})", request.getModel(), fallbackModel, depth);
                request.setModel(fallbackModel);
                currentCandidates = routingResolver.resolveCandidates(fallbackModel, inboundProtocol,
                        identity.applicationId(), identity.userId(), identity.role(), strategy);
                if (currentCandidates == null || currentCandidates.isEmpty()) {
                    log.warn("流式 L2 降级：fallback 模型 {} 无可用候选，抛出原始上游异常", fallbackModel);
                    throw unwrapL2Cause(e);
                }
                currentPrimaryCtx = currentCandidates.get(0);
            }
        }
        log.warn("流式 L2 降级深度超限 (max={})，抛出原始上游异常", maxDepth);
        throw unwrapL2Cause(lastL2Signal);
    }

    /**
     * 解析容灾画像（fail-open，Task 4.9）
     *
     * <p>applicationId 为 null 或画像解析抛异常时返回 null，避免画像解析失败阻断主调度链。
     * 与 {@link InstanceSelector#resolveProfileSafely} 同语义，调度层与路由层各自 fail-open 解析。</p>
     *
     * @param applicationId 应用 ID
     * @return 容灾画像；解析失败或无应用 ID 时返回 null
     */
    private ResilienceProfile resolveProfileSafely(Long applicationId) {
        if (applicationId == null) {
            return null;
        }
        try {
            return resilienceResolver.resolve(applicationId);
        } catch (Exception e) {
            log.debug("容灾画像解析失败，降级为 null profile（fail-open）: applicationId={}, reason={}",
                    applicationId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析 L2 重路由深度上限（Task 4.9）
     *
     * <p>画像 degradationMaxDepth 优先（>0 时取画像值）；无画像或画像值为 0/负时回退默认
     * {@link #MAX_DEGRADATION_DEPTH}。与 {@code DegradationService.degrade} 的备选遍历深度
     * 语义一致：画像门禁生效，无画像回退既有默认。</p>
     *
     * @param profile 容灾画像
     * @return L2 重路由深度上限
     */
    private int resolveMaxDepth(ResilienceProfile profile) {
        if (profile != null && profile.getDegradationMaxDepth() > 0) {
            return profile.getDegradationMaxDepth();
        }
        return MAX_DEGRADATION_DEPTH;
    }

    /**
     * 解包 L2 降级信号的原始上游异常
     *
     * <p>防递归/深度超限/无候选时调用，从 {@link L2DegradationRequiredException#getCause()}
     * 取出原始 ProviderException 抛出，保留上游失败上下文。cause 非 ProviderException 或为 null 时
     * 兜底构造 UPSTREAM_ERROR 异常。</p>
     *
     * @param e L2 降级信号（可为 null，深度超限且无信号时）
     * @return 原始上游异常或兜底异常
     */
    private ProviderException unwrapL2Cause(L2DegradationRequiredException e) {
        Throwable cause = e != null ? e.getCause() : null;
        if (cause instanceof ProviderException pe) {
            return pe;
        }
        return new ProviderException(ProviderErrorType.UPSTREAM_ERROR,
                e != null ? e.getMessage() : "L2 降级失败：无原始异常上下文");
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
