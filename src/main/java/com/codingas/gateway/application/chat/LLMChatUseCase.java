package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.supply.entity.RouteGroup;
import com.codingas.gateway.domain.supply.service.LLMDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * LLM 聊天用例编排器
 *
 * <p>Application 层用例编排，负责 LLM 聊天请求的处理。</p>
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
     * @param onChunk 流式响应回调
     */
    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        log.debug("UseCase: send stream request, model={}, strategy={}", request.getModel(), strategy);
        llmDispatcher.sendStream(request, strategy, onChunk);
    }

    /**
     * 发布 Token 使用事件
     */
    private void publishTokenUsedEvent(LLMRequest request, LLMResponse response) {
        if (response != null && response.getUsage() != null) {
            var event = com.codingas.gateway.common.event.TokenUsedEvent.builder()
                    .model(request.getModel())
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .cost(BigDecimal.ZERO)
                    .traceId(null)
                    .occurredOn(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("Published TokenUsedEvent for model={}", request.getModel());
        }
    }
}
