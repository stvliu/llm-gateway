package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;

import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天服务接口
 *
 * <p>Application 层统一入口，编排代理请求处理：</p>
 * <ul>
 *   <li>模型路由选择</li>
 *   <li>调用 LLM Dispatcher 发送请求</li>
 *   <li>发布 Token 使用事件</li>
 * </ul>
 */
public interface ChatService {

    /**
     * 发送非流式请求（使用 LLMRequest）
     *
     * @param request  LLM 请求
     * @param strategy 路由策略
     * @return LLM 响应
     */
    LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy);

    /**
     * 发送流式请求（使用 LLMRequest）
     *
     * @param request  LLM 请求
     * @param strategy 路由策略
     * @param onChunk  流式响应回调
     */
    void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk);

    /**
     * 发送流式请求（带完成和错误回调）
     *
     * @param request    LLM 请求
     * @param strategy   路由策略
     * @param onChunk    流式响应回调
     * @param onComplete 完成回调
     * @param onError    错误回调
     */
    void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy,
                    Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError);

    /**
     * 处理聊天请求（简化版，使用默认策略）
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 处理流式聊天请求（简化版，使用默认策略）
     *
     * @param request 聊天请求
     * @param onChunk 流式响应回调
     */
    void chatStream(ChatRequest request, Consumer<String> onChunk);

    /**
     * 简化版聊天请求
     */
    record ChatRequest(
            String model,
            List<LLMRequest.Message> messages,
            RouteGroup.RoutingStrategy strategy
    ) {
        public ChatRequest(String model, List<LLMRequest.Message> messages) {
            this(model, messages, RouteGroup.RoutingStrategy.WEIGHTED);
        }
    }

    /**
     * 简化版聊天响应
     */
    record ChatResponse(String model, String content) {}
}
