package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.service.ModelRouterDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * 聊天用例应用服务实现
 *
 * <p>编排聊天请求处理，调用多个领域服务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ModelRouterDomainService modelRouterService;
    private final LLMChatUseCase llmChatUseCase;

    /**
     * 处理聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
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
                : RouteGroup.RoutingStrategy.COST_OPTIMIZED;

        LLMResponse response = llmChatUseCase.send(llmRequest, strategy);

        log.info("Chat request processed: model={}", selectedModel.getModelCode());
        String content = extractContent(response);
        return new ChatResponse(selectedModel.getModelCode(), content);
    }

    /**
     * 处理流式聊天请求
     *
     * @param request 聊天请求
     * @param onChunk 流式响应回调
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
                : RouteGroup.RoutingStrategy.COST_OPTIMIZED;

        llmChatUseCase.sendStream(llmRequest, strategy, onChunk);
        log.info("Stream chat request processed: model={}", selectedModel.getModelCode());
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
