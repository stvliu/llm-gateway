package com.codingas.gateway.dispatch;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.StreamCallback;
import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.core.domain.entity.RouteGroup.RoutingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * LLM 调度器
 *
 * <p>协调模型路由、适配器调用和响应转换。</p>
 * <p>主要职责：</p>
 * <ul>
 *   <li>根据策略选择合适的 Provider 适配器</li>
 *   <li>调用适配器发送请求</li>
 *   <li>处理流式和非流式响应</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMDispatcher {

    private final ModelRouter modelRouter;
    private final ProtocolTranslator protocolTranslator;

    /**
     * 发送非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return LLM 响应
     */
    public LLMResponse send(LLMRequest request, RoutingStrategy strategy) {
        // 1. 路由选择
        LLMProviderAdapter adapter = modelRouter.select(request, strategy);
        log.info("Dispatching request to adapter: {}, model: {}",
                adapter.getProviderCode(), request.getModel());

        // 2. 选择调用方法
        if (adapter.getProviderType() == com.codingas.gateway.adapter.common.ProviderType.ANTHROPIC) {
            // Anthropic Provider 使用 messages API
            return adapter.messages(request);
        } else {
            // OpenAI 和其他 Provider 使用 chat API
            return adapter.chat(request);
        }
    }

    /**
     * 发送流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @param callback 流式回调
     */
    public void sendStream(LLMRequest request, RoutingStrategy strategy, StreamCallback callback) {
        // 1. 路由选择
        LLMProviderAdapter adapter = modelRouter.select(request, strategy);
        log.info("Dispatching stream request to adapter: {}, model: {}",
                adapter.getProviderCode(), request.getModel());

        // 2. 选择调用方法
        if (adapter.getProviderType() == com.codingas.gateway.adapter.common.ProviderType.ANTHROPIC) {
            adapter.messagesStream(request, callback);
        } else {
            adapter.chatStream(request, callback);
        }
    }

    /**
     * 异步发送非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return CompletableFuture 包装的响应
     */
    public CompletableFuture<LLMResponse> sendAsync(LLMRequest request, RoutingStrategy strategy) {
        return CompletableFuture.supplyAsync(() -> send(request, strategy));
    }
}
