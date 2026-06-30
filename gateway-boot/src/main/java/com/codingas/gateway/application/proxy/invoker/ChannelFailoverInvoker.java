package com.codingas.gateway.application.proxy.invoker;

import com.codingas.gateway.application.proxy.OutboundTuner;
import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesRequest;
import com.codingas.gateway.domain.protocol.contract.AnthropicMessagesResponse;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatRequest;
import com.codingas.gateway.domain.protocol.contract.OpenAIChatResponse;
import com.codingas.gateway.domain.protocol.contract.ProtocolRequest;
import com.codingas.gateway.domain.protocol.contract.ProtocolResponse;
import com.codingas.gateway.domain.protocol.contract.StreamCallback;
import com.codingas.gateway.domain.protocol.contract.StreamChunkResult;
import com.codingas.gateway.domain.protocol.conversion.ProtocolConverter;
import com.codingas.gateway.domain.supply.enums.FailoverDecision;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.domain.supply.exception.ProviderException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 渠道级故障转移 Invoker（L1 候选内逐个试，耗尽抛最后异常）
 *
 * <p>遍历候选渠道列表（同模型不同渠道，已按 priority 升序），对每个候选调用
 * {@link KeyFailoverInvoker}（内部跑 L0 Key 级转移）。捕获 ProviderException 后
 * 经 {@link ErrorClassifier} 分类决定转移层级（D3/D5/深化点5）：</p>
 *
 * <ul>
 *   <li>NONE（请求级错误如 INVALID_REQUEST）：直接抛出，不试下一候选</li>
 *   <li>L1（共因故障如 AUTH/RATE_LIMIT/NETWORK）：换下一候选，全部耗尽后抛最后捕获的异常</li>
 * </ul>
 *
 * <p><b>Task 4 变更</b>：L2 模型降级层已删除。候选全部耗尽后直接抛最后捕获的上游异常，
 * 不再进入 L2 模型降级（降级决策交还给应用层）。容灾栈从四层（L0/L1/L2/L3）收敛为三层（L0/L1/L3）。</p>
 */
@Component
public class ChannelFailoverInvoker {

    private static final Logger log = LoggerFactory.getLogger(ChannelFailoverInvoker.class);

    private final KeyFailoverInvoker keyFailoverInvoker;
    private final ErrorClassifier errorClassifier;
    private final DomainEventPublisher eventPublisher;
    private final ChannelGateway channelGateway;
    /** 出站调谐编排器（调谐下沉：每候选独立 convert+tune） */
    private final OutboundTuner outboundTuner;
    /** 跨协议转换器（调谐下沉：每候选独立请求转换 + 流式 chunk 转换方向重建） */
    private final ProtocolConverter protocolConverter;

    /**
     * 构造渠道级故障转移 Invoker
     *
     * @param keyFailoverInvoker  Key 级故障转移 Invoker（L0，对每个候选内部跑）
     * @param errorClassifier     错误分流器（L1/NONE 决策）
     * @param eventPublisher      领域事件发布器（发布转移事件，供异步持久化与可观测性）
     * @param channelGateway      渠道网关（反查 channelId→clusterId 填充转移事件，使 clusterId 过滤生效）
     * @param outboundTuner       出站调谐编排器（每候选独立 tune：协议级默认值补全 + 模型名替换）
     * @param protocolConverter   跨协议转换器（每候选独立 convertRequest + 流式 chunk 转换方向重建）
     */
    public ChannelFailoverInvoker(KeyFailoverInvoker keyFailoverInvoker,
                                   ErrorClassifier errorClassifier,
                                   DomainEventPublisher eventPublisher,
                                   ChannelGateway channelGateway,
                                   OutboundTuner outboundTuner,
                                   ProtocolConverter protocolConverter) {
        this.keyFailoverInvoker = keyFailoverInvoker;
        this.errorClassifier = errorClassifier;
        this.eventPublisher = eventPublisher;
        this.channelGateway = channelGateway;
        this.outboundTuner = outboundTuner;
        this.protocolConverter = protocolConverter;
    }

    /**
     * 非流式调用 — L1 候选内逐个试，耗尽抛最后异常
     *
     * <p>遍历 candidates（已按 priority 升序），对每个候选调 {@link KeyFailoverInvoker#invoke}：
     * 成功返回；失败按 {@link ErrorClassifier#classify} 分类——NONE 直接抛，
     * L1 换下一候选。全部候选耗尽后直接抛最后捕获的上游异常（L2 模型降级层已删除，
     * 不再换模型，降级决策交还给应用层）。</p>
     *
     * @param primaryCtx               主路由上下文（候选列表首项，用于日志/审计锚点）
     * @param candidates               按 priority 升序的候选路由上下文列表（由调用方传入）
     * @param request                  协议请求
     * @param inboundProtocol          入站协议
     * @param applicationId            应用 ID（权限锚点）
     * @param traceId                  调用链 Trace ID（由上层 ChatDispatchServiceImpl 生成），透传到转移事件
     *                                  串联同请求多次转移；为 null 时事件 traceId 字段为 null
     * @return 上游响应
     * @throws ProviderException  INVALID_REQUEST 等请求级错误直接抛出；
     *                            所有候选失败时抛出最后捕获的异常
     */
    public ProtocolResponse invoke(RoutingContext primaryCtx, List<RoutingContext> candidates,
                                    ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                                    String traceId) {
        ProviderException lastException = null;

        for (int i = 0; i < candidates.size(); i++) {
            RoutingContext candidate = candidates.get(i);
            try {
                // 调谐下沉：每候选基于原始 request 副本独立 convert+tune（修复 L1 换渠道后 model 错误）
                ProtocolRequest candidateReq = adaptRequestForCandidate(request, candidate);
                ProtocolResponse response = keyFailoverInvoker.invoke(candidate, candidateReq);
                // 响应转换下沉：基于实际成功候选(非主候选) 转换响应为入站协议格式（与流式 buildStreamCallback 对称）
                // 修复跨协议换候选时按主候选协议转换方向错误/跳过转换，返回错误协议响应的问题
                return adaptResponseForCandidate(response, candidate);
            } catch (ProviderException e) {
                FailoverDecision decision = errorClassifier.classify(e.getErrorType());
                log.warn("候选渠道 channelId={} endpointId={} 失败: {} (决策:{}), 尝试下一候选",
                        candidate.channelId(), candidate.channelEndpointId(),
                        e.getErrorType(), decision);

                // NONE：请求级错误（如 INVALID_REQUEST），换哪都无效，直接抛出不转移
                if (decision == FailoverDecision.NONE) {
                    throw e;
                }

                // L1：发布转移事件（换下一候选前），再记录失败继续试下一候选
                publishFailoverEvent(candidate, candidates, i, applicationId, e.getErrorType(), decision, traceId);
                lastException = e;
            }
        }

        // L1 候选全部耗尽：直接抛最后捕获的上游异常（L2 模型降级层已删除，不再换模型）
        if (lastException != null) {
            throw lastException;
        }
        // 防御性兜底：候选列表非空但无成功无异常（理论不可达）
        throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
                "候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
    }

    /**
     * 流式调用 — L1 候选内逐个试（首字节前可转移，首字节后不换渠道），耗尽抛最后异常
     *
     * <p>遍历 candidates，对每个候选调 {@link KeyFailoverInvoker#invokeStream}。
     * 包装传入的 callback 追踪首字节是否已发送（首次 {@link StreamCallback#onChunk} 标记）。
     * 首字节前同步启动失败（invokeStream 抛 ProviderException 且 onChunk 未触发）按分类转移：
     * NONE 直接抛，L1 换下一候选。若 onChunk 已触发（首字节已发给客户端），catch 块检查
     * firstByteSent 后直接抛出不换候选（避免重复首字节）。一旦 invokeStream 正常 return 表示
     * 流已建立，ChannelFailoverInvoker 即返回；后续首字节前异步失败或首字节后失败均通过
     * wrappedCallback 转发原 callback 的 onError，不再换渠道（继承 KeyFailoverInvoker
     * "传输开始后不切换"约束）。</p>
     *
     * <p>全部候选启动失败后直接抛最后捕获的上游异常（L2 模型降级层已删除，不再换模型）。</p>
     *
     * @param primaryCtx               主路由上下文
     * @param candidates               按 priority 升序的候选列表
     * @param request                  协议请求
     * @param inboundProtocol          入站协议
     * @param applicationId            应用 ID
     * @param traceId                  调用链 Trace ID（由上层 ChatDispatchServiceImpl 生成），透传到转移事件
     *                                  串联同请求多次转移；为 null 时事件 traceId 字段为 null
     * @param callback                 流式回调
     * @throws ProviderException  INVALID_REQUEST 等请求级错误直接抛出；
     *                            所有候选启动失败时抛出最后捕获的异常
     */
    public void invokeStream(RoutingContext primaryCtx, List<RoutingContext> candidates,
                              ProtocolRequest request, Protocol inboundProtocol, Long applicationId,
                              String traceId, StreamCallback callback) {
        ProviderException lastException = null;

        for (int i = 0; i < candidates.size(); i++) {
            RoutingContext candidate = candidates.get(i);
            // 首字节追踪标志：包装 callback 标记首字节是否已发送
            // 首字节前同步启动失败可换候选；首字节后失败不换候选（继承 KeyFailoverInvoker 约束）
            AtomicBoolean firstByteSent = new AtomicBoolean(false);
            // 流式 chunk 转换方向基于实际候选 upstreamProtocol 重建（非主候选）：
            // 换候选仅发生在首字节前（callback 未触发），故可安全按成功候选协议构造转换方向，
            // 修复跨协议换候选时 delegateCallback 按主候选协议转换方向错误的问题。
            StreamCallback candidateCallback = buildStreamCallback(candidate, inboundProtocol, callback);
            StreamCallback wrappedCallback = new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    // 首次 onChunk 标记首字节已发送
                    firstByteSent.set(true);
                    candidateCallback.onChunk(data);
                }

                @Override
                public void onComplete() {
                    candidateCallback.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    candidateCallback.onError(t);
                }
            };

            try {
                // 调谐下沉：每候选基于原始 request 副本独立 convert+tune
                ProtocolRequest candidateReq = adaptRequestForCandidate(request, candidate);
                // KeyFailoverInvoker.invokeStream 正常 return 表示流已建立（enqueue 成功）
                // 首字节前的异步失败（onError 在 onChunk 前）和首字节后失败均通过 wrappedCallback
                // 转发原 callback，ChannelFailoverInvoker 已返回不再换候选
                keyFailoverInvoker.invokeStream(candidate, candidateReq, wrappedCallback);
                return;
            } catch (ProviderException e) {
                // 首字节已发送：不换候选，直接抛传播给调用方（首字节后转移边界）
                // 客户端已收到首字节，换候选重发会导致重复首字节，故直接终止
                if (firstByteSent.get()) {
                    throw e;
                }

                // 首字节前同步启动失败：按 L1/NONE 分流换候选
                FailoverDecision decision = errorClassifier.classify(e.getErrorType());
                log.warn("流式候选渠道 channelId={} endpointId={} 启动失败: {} (决策:{}), 尝试下一候选",
                        candidate.channelId(), candidate.channelEndpointId(),
                        e.getErrorType(), decision);

                // NONE：请求级错误，直接抛出不转移
                if (decision == FailoverDecision.NONE) {
                    throw e;
                }

                // L1：发布转移事件（换下一候选前），再记录失败继续试下一候选（首字节前失败可转移）
                publishFailoverEvent(candidate, candidates, i, applicationId, e.getErrorType(), decision, traceId);
                lastException = e;
            }
        }

        // L1 候选全部启动失败：直接抛最后捕获的上游异常（L2 模型降级层已删除，不再换模型）
        if (lastException != null) {
            throw lastException;
        }
        throw new ProviderException(ProviderErrorType.UNKNOWN_ERROR,
                "流式候选列表处理完成但无结果：candidates=" + (candidates == null ? 0 : candidates.size()));
    }

    /**
     * 调谐下沉：基于原始请求副本为单个候选执行 convert+tune
     *
     * <p>每候选独立处理，避免候选间相互污染（如模型名替换覆盖原始请求）：</p>
     * <ol>
     *   <li>{@code original.copy()} 生成同类型副本（手写字段拷贝）</li>
     *   <li>若候选需跨协议适配（{@link RoutingContext#needsProtocolAdaptation()}），
     *       调 {@link ProtocolConverter} 转换请求协议</li>
     *   <li>{@link OutboundTuner#tune} 执行协议级默认值补全 + 模型名替换（候选 upstreamModelName）</li>
     * </ol>
     *
     * @param original  原始入站请求（不变，仅读取）
     * @param candidate 当前候选路由上下文
     * @return 该候选的出站请求（副本，已 convert+tune）
     */
    private ProtocolRequest adaptRequestForCandidate(ProtocolRequest original, RoutingContext candidate) {
        ProtocolRequest candidateReq = original.copy();
        if (candidate.needsProtocolAdaptation()) {
            candidateReq = convertRequest(candidateReq, candidate);
        }
        return outboundTuner.tune(candidateReq, candidate);
    }

    /**
     * 跨协议请求转换（下沉自 ChatDispatchServiceImpl，每候选独立执行）
     *
     * @param request 协议请求（已 copy）
     * @param ctx     路由上下文（提供目标上游协议）
     * @return 转换后的请求；无需转换时返回原请求
     */
    private ProtocolRequest convertRequest(ProtocolRequest request, RoutingContext ctx) {
        if (request instanceof OpenAIChatRequest openai && ctx.upstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toAnthropic(openai);
        }
        if (request instanceof AnthropicMessagesRequest anthropic && ctx.upstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toOpenAI(anthropic);
        }
        return request;
    }

    /**
     * 响应转换下沉：基于实际成功候选将上游响应转换为入站协议格式
     *
     * <p>与 {@link #adaptRequestForCandidate} 对称，下沉自 {@code ChatDispatchServiceImpl.convertResponse}。
     * 关键差异：基于<b>实际成功候选</b>（非主候选）的 upstreamProtocol 决定转换方向，确保 L1 换到
     * 跨协议备候选时响应被正确转换为入站协议格式。修复前响应转换留在 dispatch 阶段6 且基于主候选：
     * 当主候选同协议(needsAdaptation=false)而实际成功的是跨协议备候选时，阶段6 跳过转换，
     * 返回错误协议响应，违反双 API 兼容铁律。</p>
     *
     * <p>同协议候选(needsProtocolAdaptation=false)不转换，原样返回。</p>
     *
     * @param response  上游响应
     * @param candidate 实际成功候选路由上下文
     * @return 入站协议格式的响应；同协议候选原样返回
     */
    private ProtocolResponse adaptResponseForCandidate(ProtocolResponse response, RoutingContext candidate) {
        if (!candidate.needsProtocolAdaptation()) {
            return response;
        }
        return convertResponse(response, candidate);
    }

    /**
     * 跨协议响应转换（下沉自 ChatDispatchServiceImpl，每候选独立执行）
     *
     * @param response 上游响应
     * @param ctx      路由上下文（提供上游协议，决定转换方向）
     * @return 转换后的响应；无法匹配时原样返回并告警
     */
    private ProtocolResponse convertResponse(ProtocolResponse response, RoutingContext ctx) {
        if (response instanceof AnthropicMessagesResponse anthropic && ctx.upstreamProtocol() == Protocol.ANTHROPIC) {
            return protocolConverter.toOpenAI(anthropic);
        }
        if (response instanceof OpenAIChatResponse openai && ctx.upstreamProtocol() == Protocol.OPENAI) {
            return protocolConverter.toAnthropic(openai);
        }
        log.warn("无法转换响应: upstreamProtocol={}, 原样返回", ctx.upstreamProtocol());
        return response;
    }

    /**
     * 构造流式回调：基于实际候选 upstreamProtocol 重建 chunk 转换方向
     *
     * <p>下沉自 {@code ChatDispatchServiceImpl.dispatchStream} 的 delegateCallback 构造，关键差异：
     * 转换方向基于<b>当前候选</b>的 upstreamProtocol（非主候选），确保跨协议换候选后
     * chunk 转换方向正确。换候选仅发生在首字节前（callback 未触发），故按成功候选协议
     * 构造转换方向是安全的。</p>
     *
     * <ul>
     *   <li>跨协议候选：chunk 经 {@link ProtocolConverter#convertStreamChunk} 转换，
     *       onComplete 经 {@link ProtocolConverter#convertStreamDone} 转换结束标记</li>
     *   <li>同协议候选：chunk 透传，OpenAI 入站时 onComplete 注入 [DONE] 结束标记</li>
     * </ul>
     *
     * @param candidate       当前候选路由上下文
     * @param inboundProtocol 入站协议
     * @param downstream      下游回调（dispatchStream 的 auditingCallback）
     * @return 基于候选协议的流式回调
     */
    private StreamCallback buildStreamCallback(RoutingContext candidate, Protocol inboundProtocol,
                                               StreamCallback downstream) {
        String fromProtocol = candidate.upstreamProtocol().name().toLowerCase();
        String toProtocol = inboundProtocol.name().toLowerCase();
        if (candidate.needsProtocolAdaptation()) {
            // 跨协议：chunk + 结束标记转换（方向：候选 upstreamProtocol → inbound）
            return new StreamCallback() {
                @Override
                public void onChunk(String data) {
                    StreamChunkResult result = protocolConverter.convertStreamChunk(data, fromProtocol, toProtocol);
                    if (result != null) {
                        downstream.onChunk(result.data());
                    }
                }

                @Override
                public void onComplete() {
                    StreamChunkResult done = protocolConverter.convertStreamDone(fromProtocol, toProtocol);
                    if (done != null) {
                        downstream.onChunk(done.data());
                    }
                    downstream.onComplete();
                }

                @Override
                public void onError(Throwable t) {
                    downstream.onError(t);
                }
            };
        }
        // 同协议：chunk 透传，OpenAI 入站注入 [DONE] 结束标记
        return new StreamCallback() {
            @Override
            public void onChunk(String data) {
                downstream.onChunk(data);
            }

            @Override
            public void onComplete() {
                if (inboundProtocol == Protocol.OPENAI) {
                    downstream.onChunk("[DONE]");
                }
                downstream.onComplete();
            }

            @Override
            public void onError(Throwable t) {
                downstream.onError(t);
            }
        };
    }

    /**
     * 发布转移事件（Task 4.11c 容灾可观测性，design doc D12）
     *
     * <p>当 decision 非 NONE（L1 换候选）时，换下一候选前发布 {@link FailoverOccurredEvent}：
     * from=当前失败候选（candidate），to=下一候选（若已是最后候选则为 null 且 exhausted=true）。
     * 事件由 {@code FailoverEventListener} 异步持久化为 {@code FailoverEvent} 实体，
     * 不阻塞调用链（发布与持久化解耦）。</p>
     *
     * <p>traceId / fromClusterId / toClusterId 当前置空：调用链未透传 traceId（后续 task 接入
     * OpenTelemetry 后填充），{@link RoutingContext} 未携带 clusterId（未来扩展后填充）。
     * 这些字段置空不影响核心可观测性（from/to 渠道端点、errorType、decision、exhausted 已足够）。</p>
     *
     * @param candidate     当前失败的候选上下文（from）
     * @param candidates    候选列表
     * @param currentIndex  当前候选索引
     * @param applicationId 应用 ID
     * @param errorType     触发转移的上游错误类型
     * @param decision      转移决策（L1）
     * @param traceId       调用链 Trace ID，透传到事件串联同请求多次转移；为 null 时事件字段为 null
     */
    private void publishFailoverEvent(RoutingContext candidate, List<RoutingContext> candidates,
                                       int currentIndex, Long applicationId,
                                       ProviderErrorType errorType, FailoverDecision decision,
                                       String traceId) {
        // 判断是否有下一候选：有则 to=下一候选，无则 to=null + exhausted=true
        int nextIndex = currentIndex + 1;
        boolean hasTo = nextIndex < candidates.size();
        Long toChannelId = hasTo ? candidates.get(nextIndex).channelId() : null;
        Long toEndpointId = hasTo ? candidates.get(nextIndex).channelEndpointId() : null;
        boolean exhausted = !hasTo;

        // 反查 channelId→clusterId 填充冗余字段，使转移事件流 clusterId 过滤生效
        // （RoutingContext 不携带 clusterId，需经 ChannelGateway 回查；单次转移仅涉及 1-2 个渠道，
        //   单次 findById 即可，转移是失败路径非每次请求都转移，额外查询开销可接受）
        Long fromClusterId = resolveClusterId(candidate.channelId());
        Long toClusterId = hasTo ? resolveClusterId(toChannelId) : null;

        FailoverOccurredEvent event = new FailoverOccurredEvent(
                traceId,                    // traceId：由上层 ChatDispatchServiceImpl 生成并透传，串联同请求多次转移
                applicationId,
                candidate.channelId(),
                candidate.channelEndpointId(),
                toChannelId,
                toEndpointId,
                fromClusterId,              // 冗余：失败候选所属故障域（反查填充，查不到为 null）
                toClusterId,                // 冗余：转移目标所属故障域（同上，无目标时为 null）
                errorType,
                decision,
                exhausted,
                Instant.now()
        );
        eventPublisher.publish(event);
    }

    /**
     * 反查 channelId 对应的 clusterId（用于填充转移事件冗余字段，使 clusterId 过滤生效）
     *
     * <p>容错：channelId 查不到（渠道已删除或不存在）时返回 null，不阻塞事件发布。
     * 转移是失败路径，每次转移多一次 channel 查询；ChannelGateway 通常带缓存或单次查询，开销可接受。</p>
     *
     * @param channelId 渠道 ID
     * @return 故障域 ID；渠道不存在或未关联 cluster 时返回 null
     */
    private Long resolveClusterId(Long channelId) {
        if (channelId == null) {
            return null;
        }
        return channelGateway.findById(channelId)
                .map(com.codingas.gateway.domain.supply.entity.Channel::getClusterId)
                .orElse(null);
    }
}
