package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import com.codingas.gateway.domain.usage.event.TokenUsedEvent;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterBuilderFactory;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * 代理服务实现
 *
 * <p>Application 层统一入口，编排代理请求处理流程。</p>
 * <p>使用 ChannelRoutingService 进行渠道路由，AdapterBuilderFactory 动态创建适配器。</p>
 *
 * <p>处理流程：</p>
 * <ol>
 *   <li>认证检查 - 由 Adapter 层拦截器完成</li>
 *   <li>限额检查 - 由 Adapter 层拦截器完成</li>
 *   <li>路由选择 - 通过 ChannelRoutingService</li>
 *   <li>代理转发 - 通过动态创建的 LLMAdapter</li>
 *   <li>记录用量 - 发布事件</li>
 *   <li>审计日志 - 由事件监听器完成</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyServiceImpl implements ProxyService {

    private final ChannelRoutingService channelRoutingService;
    private final AdapterBuilderFactory adapterBuilderFactory;
    private final StreamCallbackFactory streamCallbackFactory;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 代理转发非流式请求
     */
    @Override
    public LLMResponse proxy(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        log.debug("Proxying request: model={}, strategy={}", request.getModel(), strategy);

        // 1. 路由解析
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);

        // 2. 创建临时适配器（动态 apiKey + baseUrl）
        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            ctx.provider().getType(),
            ctx.provider().getBaseUrl(),
            ctx.apiKey().getApiKey(),
            ctx.getTimeoutSeconds()
        );

        // 3. 执行请求
        LLMResponse response = adapter.chat(request);

        // 4. 记录用量
        publishTokenUsedEvent(request, response, ctx);

        log.info("Request processed: model={}, provider={}, channel={}",
            request.getModel(), ctx.provider().getName(), ctx.model().getId());
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

        // 1. 路由解析
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);

        // 2. 创建临时适配器（动态 apiKey + baseUrl）
        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            ctx.provider().getType(),
            ctx.provider().getBaseUrl(),
            ctx.apiKey().getApiKey(),
            ctx.getTimeoutSeconds()
        );

        // 3. 创建回调
        StreamCallback callback = streamCallbackFactory.create(onChunk, onComplete, onError);

        // 4. 执行流式请求
        adapter.chatStream(request, callback);

        log.info("Stream request processed: model={}, provider={}, channel={}",
            request.getModel(), ctx.provider().getName(), ctx.model().getId());
    }

    /**
     * 发布 Token 使用事件
     */
    private void publishTokenUsedEvent(LLMRequest request, LLMResponse response, RoutingContext ctx) {
        if (response != null && response.getUsage() != null) {
            var event = TokenUsedEvent.builder()
                    .model(request.getModel())
                    .promptTokens(response.getUsage().getPromptTokens())
                    .completionTokens(response.getUsage().getCompletionTokens())
                    .cost(calculateCost(ctx, response))
                    .traceId(null)
                    .occurredOn(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("Published TokenUsedEvent for model={}", request.getModel());
        }
    }

    /**
     * 计算请求成本
     */
    private BigDecimal calculateCost(RoutingContext ctx, LLMResponse response) {
        if (ctx.model() == null || response.getUsage() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;

        if (ctx.model().getInputPrice() != null) {
            inputCost = ctx.model().getInputPrice()
                .multiply(BigDecimal.valueOf(response.getUsage().getPromptTokens()))
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        if (ctx.model().getOutputPrice() != null) {
            outputCost = ctx.model().getOutputPrice()
                .multiply(BigDecimal.valueOf(response.getUsage().getCompletionTokens()))
                .divide(BigDecimal.valueOf(1_000_000), 10, RoundingMode.HALF_UP);
        }

        return inputCost.add(outputCost);
    }
}
