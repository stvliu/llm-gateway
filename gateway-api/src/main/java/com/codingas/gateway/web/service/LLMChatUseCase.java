package com.codingas.gateway.web.service;

import com.codingas.gateway.adapter.StreamCallback;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.core.domain.event.TokenUsedEvent;
import com.codingas.gateway.router.LLMDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * LLM 聊天用例编排器
 *
 * <p>Application 层用例编排，无业务逻辑。</p>
 * <p>职责：协调路由、LLM 调用和事件发布。</p>
 *
 * <p>遵循 COLA 5.0 架构：
 * <ul>
 *   <li>接收 Controller 层的 DTO</li>
 *   <li>编排 Domain Service（通过 LLMDispatcher）</li>
 *   <li>发布领域事件（TokenUsedEvent）用于异步统计</li>
 *   <li>返回响应 DTO</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMChatUseCase {

    private final LLMDispatcher llmDispatcher;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 发送非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return LLM 响应
     */
    public LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("UseCase: send request, model={}, strategy={}", request.getModel(), strategy);

        LLMResponse response = llmDispatcher.send(request, strategy);

        publishTokenUsedEvent(request, response);

        return response;
    }

    /**
     * 发送流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @param callback 流式回调
     */
    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, StreamCallback callback) {
        log.debug("UseCase: send stream request, model={}, strategy={}", request.getModel(), strategy);
        llmDispatcher.sendStream(request, strategy, callback);
    }

    /**
     * 异步发送非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return CompletableFuture 包装的响应
     */
    public CompletableFuture<LLMResponse> sendAsync(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        return CompletableFuture.supplyAsync(() -> send(request, strategy));
    }

    /**
     * 发布 Token 使用事件
     *
     * <p>异步统计 Token 使用量，用于预算控制和审计。</p>
     */
    private void publishTokenUsedEvent(LLMRequest request, LLMResponse response) {
        if (response.getUsage() != null) {
            TokenUsedEvent event = TokenUsedEvent.builder()
                    .model(request.getModel())
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .cost(calculateCost(request, response))
                    .traceId(getTraceId())
                    .occurredOn(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("Published TokenUsedEvent for model={}", request.getModel());
        }
    }

    /**
     * 计算费用
     *
     * <p>TODO: 基于模型定价计算实际费用</p>
     */
    private BigDecimal calculateCost(LLMRequest request, LLMResponse response) {
        // TODO: 从 ModelGateway 获取模型定价计算
        return BigDecimal.ZERO;
    }

    /**
     * 获取当前 Trace ID
     *
     * <p>TODO: 从 OpenTelemetry Context 获取</p>
     */
    private String getTraceId() {
        // TODO: 从 OpenTelemetry Context 获取
        return null;
    }
}
