package com.codingas.gateway.application.chat;

import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.service.ModelRouterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 聊天用例应用服务
 *
 * <p>编排聊天请求处理，调用多个领域服务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplication {

    private final ModelRouterService modelRouterService;
    // 后续会添加更多依赖：AuthenticationService, LLMProviderService 等

    /**
     * 处理聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    public ChatResponse chat(ChatRequest request) {
        // 1. 路由选择模型
        Model selectedModel = modelRouterService.selectModel(request.model());

        // 2. TODO: 调用 LLM 提供商

        // 3. 返回响应
        log.info("Chat request processed: model={}, message={}",
            selectedModel.getModelCode(), request.message());

        return new ChatResponse(
            selectedModel.getModelCode(),
            "Hello, this is a placeholder response"
        );
    }

    public record ChatRequest(String model, String message) {}
    public record ChatResponse(String model, String content) {}
}
