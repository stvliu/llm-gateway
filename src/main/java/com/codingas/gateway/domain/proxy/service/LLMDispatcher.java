package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.LLMProviderPort;
import com.codingas.gateway.domain.proxy.gateway.ModelRouter;
import com.codingas.gateway.infrastructure.adapter.StreamCallback;
import com.codingas.gateway.infrastructure.adapter.StreamCallbackImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * LLM 请求调度器
 *
 * <p>根据路由策略将请求分发到合适的 LLM 提供商。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMDispatcher {

    private final ModelRouter modelRouter;

    /**
     * 发送非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return LLM 响应
     */
    public LLMResponse send(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("Dispatching request, model={}, strategy={}", request.getModel(), strategy);

        LLMProviderPort adapter = modelRouter.select(request, strategy);
        log.debug("Selected provider: {}", adapter.getProviderCode());

        return adapter.chat(request);
    }

    /**
     * 发送流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @param onChunk 流式响应回调
     */
    public void sendStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        log.debug("Dispatching stream request, model={}, strategy={}", request.getModel(), strategy);

        LLMProviderPort adapter = modelRouter.select(request, strategy);
        log.debug("Selected provider for stream: {}", adapter.getProviderCode());

        StreamCallback callback = new StreamCallbackImpl(onChunk);
        adapter.chatStream(request, callback);
    }
}
