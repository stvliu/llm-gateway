package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.common.event.TokenUsedEvent;
import com.codingas.gateway.domain.model.service.ModelDomainService;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import com.codingas.gateway.domain.proxy.service.ProxyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * 代理服务实现
 *
 * <p>Application 层统一入口，编排代理请求处理流程。</p>
 * <p>只调用 Domain Service，不直接访问 Gateway。</p>
 *
 * <p>处理流程（按架构定义）：</p>
 * <ol>
 *   <li>认证检查 - 由 Adapter 层拦截器完成</li>
 *   <li>限额检查 - 由 Adapter 层拦截器完成</li>
 *   <li>路由选择 - 通过 ModelDomainService 和 ProxyDomainService</li>
 *   <li>代理转发 - 通过 ProxyDomainService</li>
 *   <li>记录用量 - 发布事件</li>
 *   <li>审计日志 - 由事件监听器完成</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyServiceImpl implements ProxyService {

    private final ModelDomainService modelDomainService;
    private final ProxyDomainService proxyDomainService;
    private final StreamCallbackFactory streamCallbackFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 代理转发非流式请求
     */
    @Override
    public LLMResponse proxy(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("Proxying request: model={}, strategy={}", request.getModel(), strategy);

        // 1. 路由选择：获取模型和提供商信息（通过 Domain Service）
        ModelDomainService.ModelProviderInfo modelInfo = modelDomainService.getModelWithProviderByProviderModelId(request.getModel());

        // 2. 路由选择：获取可用的 LLM Gateway（通过 Domain Service）
        LLMGateway gateway = proxyDomainService.selectGateway(modelInfo.provider().getType());

        // 3. 代理转发（通过 Domain Service）
        LLMResponse response = proxyDomainService.forward(gateway, request);

        // 4. 记录用量
        publishTokenUsedEvent(request, response);

        log.info("Request processed: model={}, provider={}", request.getModel(), gateway.getProviderCode());
        return response;
    }

    /**
     * 代理转发流式请求
     */
    @Override
    public void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        proxyStream(request, strategy, onChunk, () -> {}, e -> log.error("Stream error: {}", e.getMessage()));
    }

    /**
     * 代理转发流式请求（带完成和错误回调）
     */
    @Override
    public void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        log.debug("Proxying stream request: model={}, strategy={}", request.getModel(), strategy);

        // 1. 路由选择：获取模型和提供商信息（通过 Domain Service）
        ModelDomainService.ModelProviderInfo modelInfo = modelDomainService.getModelWithProviderByProviderModelId(request.getModel());

        // 2. 路由选择：获取可用的 LLM Gateway（通过 Domain Service）
        LLMGateway gateway = proxyDomainService.selectGateway(modelInfo.provider().getType());

        // 3. 创建回调
        StreamCallback callback = streamCallbackFactory.create(onChunk, onComplete, onError);

        // 4. 代理转发（通过 Domain Service）
        proxyDomainService.forwardStream(gateway, request, callback);

        log.info("Stream request processed: model={}, provider={}", request.getModel(), gateway.getProviderCode());
    }

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
}
