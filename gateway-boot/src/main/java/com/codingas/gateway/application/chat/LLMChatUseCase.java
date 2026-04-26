package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * LLM 聊天用例编排器
 *
 * <p>Application 层用例编排，无业务逻辑。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMChatUseCase {

    private final ApplicationEventPublisher eventPublisher;

    public LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("UseCase: send request, model={}, strategy={}", request.getModel(), strategy);
        // TODO: 委托给 LLMDispatcher
        throw new UnsupportedOperationException("LLMDispatcher not yet implemented");
    }

    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Object callback) {
        log.debug("UseCase: send stream request, model={}, strategy={}", request.getModel(), strategy);
        throw new UnsupportedOperationException("LLMDispatcher not yet implemented");
    }

    private void publishTokenUsedEvent(LLMRequest request, LLMResponse response) {
        if (response.getUsage() != null) {
            var event = com.codingas.gateway.common.event.TokenUsedEvent.builder()
                    .model(request.getModel())
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .cost(BigDecimal.ZERO)
                    .traceId(null)
                    .occurredOn(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
        }
    }
}
