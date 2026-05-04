package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.model.entity.Model;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.service.LLMDispatcher;
import com.codingas.gateway.domain.proxy.service.ModelRouterDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * 聊天服务实现
 *
 * <p>Application 层统一入口，编排代理请求处理：</p>
 * <ul>
 *   <li>模型路由选择（通过 ModelRouterDomainService）</li>
 *   <li>调用 LLM Dispatcher 发送请求</li>
 *   <li>发布 Token 使用事件</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ModelRouterDomainService modelRouterService;
    private final LLMDispatcher llmDispatcher;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== LLMRequest 直接调用方法 ====================

    /**
     * 发送非流式请求
     *
     * <p>直接使用 LLMRequest，跳过模型路由选择（请求中已指定模型）。</p>
     */
    @Override
    public LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("Sending request: model={}, strategy={}", request.getModel(), strategy);

        LLMResponse response = llmDispatcher.send(request, strategy);
        publishTokenUsedEvent(request, response);
        return response;
    }

    /**
     * 发送流式请求
     */
    @Override
    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        sendStream(request, strategy, onChunk, () -> {}, e -> log.error("Stream error: {}", e.getMessage()));
    }

    /**
     * 发送流式请求（带完成和错误回调）
     */
    @Override
    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy,
                          Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        log.debug("Sending stream request: model={}, strategy={}", request.getModel(), strategy);
        llmDispatcher.sendStream(request, strategy, onChunk, onComplete, onError);
    }

    // ==================== 简化版 ChatRequest 方法 ====================

    /**
     * 处理聊天请求（简化版）
     *
     * <p>包含模型路由选择逻辑。</p>
     */
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.debug("Processing chat request: model={}", request.model());

        // 1. 路由选择模型
        Model selectedModel = modelRouterService.selectModel(request.model());

        // 2. 构建 LLM 请求
        LLMRequest llmRequest = LLMRequest.builder()
                .model(selectedModel.getModelCode())
                .messages(request.messages())
                .build();

        // 3. 调用 LLM
        RouteGroup.RoutingStrategy strategy = request.strategy() != null
                ? request.strategy()
                : RouteGroup.RoutingStrategy.WEIGHTED;

        LLMResponse response = send(llmRequest, strategy);

        log.info("Chat request processed: model={}", selectedModel.getModelCode());
        String content = extractContent(response);
        return new ChatResponse(selectedModel.getModelCode(), content);
    }

    /**
     * 处理流式聊天请求（简化版）
     */
    @Override
    public void chatStream(ChatRequest request, Consumer<String> onChunk) {
        log.debug("Processing stream chat request: model={}", request.model());

        // 1. 路由选择模型
        Model selectedModel = modelRouterService.selectModel(request.model());

        // 2. 构建 LLM 请求
        LLMRequest llmRequest = LLMRequest.builder()
                .model(selectedModel.getModelCode())
                .messages(request.messages())
                .stream(true)
                .build();

        // 3. 调用 LLM 流式接口
        RouteGroup.RoutingStrategy strategy = request.strategy() != null
                ? request.strategy()
                : RouteGroup.RoutingStrategy.WEIGHTED;

        sendStream(llmRequest, strategy, onChunk);
        log.info("Stream chat request processed: model={}", selectedModel.getModelCode());
    }

    // ==================== 私有方法 ====================

    /**
     * 发布 Token 使用事件
     */
    private void publishTokenUsedEvent(LLMRequest request, LLMResponse response) {
        if (response != null && response.getUsage() != null) {
            var event = TokenUsedEvent.builder()
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

    /**
     * 从 LLM 响应中提取文本内容
     */
    private String extractContent(LLMResponse response) {
        if (response == null) {
            return null;
        }
        LLMResponse.Content content = response.getContent();
        if (content == null) {
            return null;
        }
        return content.getText();
    }
}
