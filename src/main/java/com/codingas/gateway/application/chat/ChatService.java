package com.codingas.gateway.application.chat;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.router.entity.RouteGroup;

import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天用例应用服务接口
 *
 * <p>编排聊天请求处理，调用多个领域服务。</p>
 */
public interface ChatService {

    /**
     * 处理聊天请求
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 处理流式聊天请求
     *
     * @param request 聊天请求
     * @param onChunk 流式响应回调
     */
    void chatStream(ChatRequest request, Consumer<String> onChunk);

    record ChatRequest(
            String model,
            List<LLMRequest.Message> messages,
            RouteGroup.RoutingStrategy strategy
    ) {
        public ChatRequest(String model, List<LLMRequest.Message> messages) {
            this(model, messages, RouteGroup.RoutingStrategy.COST_OPTIMIZED);
        }
    }

    record ChatResponse(String model, String content) {}
}
