/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.proxy.chat;

import com.codingas.gateway.proxy.invoker.ChannelFailoverInvoker;
import com.codingas.gateway.proxy.routing.RoutingResolver;
import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.AuditGateway;
import com.codingas.gateway.protocol.*;
import com.codingas.gateway.protocol.contract.*;
import com.codingas.gateway.protocol.transport.ProviderException;
import com.codingas.gateway.protocol.transport.SseErrorFormatter;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.provider.upstream.RoutingContext;
import com.codingas.gateway.iam.valueobject.Identity;
import com.codingas.gateway.usage.event.TokenUsedEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 聊天调度服务实现
 *
 * <p>七阶段调用链：校验→路由→(转换)→调谐→调用→(转换)→后置。</p>
 *
 * <p><b>Task 4 适配</b>：L2 模型降级层已删除。调用链为：先
 * {@link RoutingResolver#resolveCandidates} 取候选列表，再直接调
 * {@link ChannelFailoverInvoker#invoke}（L1 候选内逐个试，耗尽抛最后异常）。
 * 本类不再解析容灾画像、不再做 L2 重路由循环——模型降级决策交还给应用层。
 * 容灾栈从四层（L0/L1/L2/L3）收敛为三层（L0/L1/L3）。</p>
 */
@Service
public class ChatDispatchServiceImpl implements ChatDispatchService {

    private static final Logger log = LoggerFactory.getLogger(ChatDispatchServiceImpl.class);

    private final RoutingResolver routingResolver;
    private final AuditGateway auditGateway;
    private final DomainEventPublisher eventPublisher;
    private final ChannelFailoverInvoker channelFailoverInvoker;

    public ChatDispatchServiceImpl(RoutingResolver routingResolver,
                                   AuditGateway auditGateway,
                                   DomainEventPublisher eventPublisher,
                                   ChannelFailoverInvoker channelFailoverInvoker) {
        this.routingResolver = routingResolver;
        this.auditGateway = auditGateway;
        this.eventPublisher = eventPublisher;
        this.channelFailoverInvoker = channelFailoverInvoker;
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
            // 阶段 3/4（请求转换 + 出站调谐）已下沉到 ChannelFailoverInvoker：
            // 每候选基于原始 request 副本独立 convert+tune，修复 L1 换渠道后请求 model 错误。
            // 此处直接传原始入站 request，由 Invoker 链内部按候选独立调谐。

            // 阶段 5：Invoker 链调用（熔断 + 重试 + Key 故障转移 + L1 候选转移）
            // L2 模型降级层已删除：候选耗尽直接抛最后异常，不再换模型重路由。
            ProtocolResponse response = channelFailoverInvoker.invoke(primaryCtx, candidates, request,
                    inboundProtocol, identity.applicationId(), traceId);

            // 阶段 6：响应转换已下沉 ChannelFailoverInvoker（基于实际成功候选，与流式 buildStreamCallback 对称）
            // 修复前基于主候选协议：主候选同协议(needsAdaptation=false)而实际成功的是跨协议备候选时，
            // 阶段6 跳过转换，返回错误协议响应，违反双 API 兼容铁律。

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

        // 阶段 3/4（请求转换 + 出站调谐）已下沉到 ChannelFailoverInvoker：每候选独立 convert+tune。
        // 流式 chunk 协议转换方向也由 Invoker 基于实际成功候选 upstreamProtocol 重建（非主候选），
        // 修复跨协议换候选时按主候选协议转换方向错误的问题。
        // 此处构造 auditingCallback（审计 + 用户回调），chunk 转换交给 Invoker 内部按候选重建。

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

        // 阶段 5：Invoker 链调用（L1 候选转移，首字节前启动失败可换候选）
        // L2 模型降级层已删除：候选耗尽直接抛最后异常，不再换模型重路由。
        channelFailoverInvoker.invokeStream(primaryCtx, candidates, request,
                inboundProtocol, identity.applicationId(), traceId, auditingCallback);
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

    }
