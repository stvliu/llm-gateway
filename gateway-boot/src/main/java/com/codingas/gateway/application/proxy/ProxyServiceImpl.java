package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.entity.RoutingContext;
import com.codingas.gateway.domain.proxy.gateway.StreamCallback;
import com.codingas.gateway.domain.proxy.gateway.StreamCallbackFactory;
import com.codingas.gateway.domain.security.service.UserAuthResult;
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
 * <p>支持双路路由：新架构（UserApiKey → Product）和旧架构（GatewayApiKey → Provider）。</p>
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
     * 代理转发非流式请求（带认证结果）
     */
    @Override
    public LLMResponse proxy(LLMRequest request, UserAuthResult authResult, RouteGroup.RoutingStrategy strategy) {
        RoutingContext ctx = channelRoutingService.resolve(authResult, request.getModel(), strategy);
        return doProxy(request, ctx);
    }

    /**
     * 代理转发非流式请求（旧接口，降级兼容）
     */
    @Override
    public LLMResponse proxy(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);
        return doProxy(request, ctx);
    }

    /**
     * 代理转发流式请求（带认证结果）
     */
    @Override
    public void proxyStream(LLMRequest request, UserAuthResult authResult, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        RoutingContext ctx = channelRoutingService.resolve(authResult, request.getModel(), strategy);
        doProxyStream(request, ctx, onChunk, () -> {}, e -> log.error("Stream error: {}", e.getMessage()));
    }

    /**
     * 代理转发流式请求（旧接口）
     */
    @Override
    public void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk) {
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);
        doProxyStream(request, ctx, onChunk, () -> {}, e -> log.error("Stream error: {}", e.getMessage()));
    }

    /**
     * 代理转发流式请求（旧接口，带完成和错误回调）
     */
    @Override
    public void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        RoutingContext ctx = channelRoutingService.resolve(request.getModel(), strategy);
        doProxyStream(request, ctx, onChunk, onComplete, onError);
    }

    /**
     * 代理转发流式请求（带认证结果，带完成和错误回调）
     */
    @Override
    public void proxyStream(LLMRequest request, UserAuthResult authResult, RouteGroup.RoutingStrategy strategy,
                            Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        RoutingContext ctx = channelRoutingService.resolve(authResult, request.getModel(), strategy);
        doProxyStream(request, ctx, onChunk, onComplete, onError);
    }

    /**
     * 执行非流式代理转发
     */
    private LLMResponse doProxy(LLMRequest request, RoutingContext ctx) {
        log.debug("Proxying request: model={}, newArch={}", request.getModel(), ctx.isNewArchitecture());

        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            ctx.getProviderType(),
            ctx.getEndpoint(),
            ctx.getProviderApiKey(),
            ctx.getTimeoutSeconds()
        );

        LLMResponse response = adapter.chat(request);

        publishTokenUsedEvent(request, response, ctx);

        log.info("Request processed: model={}, provider={}, newArch={}",
            request.getModel(), ctx.getProviderName(), ctx.isNewArchitecture());
        return response;
    }

    /**
     * 执行流式代理转发
     */
    private void doProxyStream(LLMRequest request, RoutingContext ctx,
                               Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError) {
        log.debug("Proxying stream request: model={}, newArch={}", request.getModel(), ctx.isNewArchitecture());

        LLMAdapter adapter = adapterBuilderFactory.createAdapter(
            ctx.getProviderType(),
            ctx.getEndpoint(),
            ctx.getProviderApiKey(),
            ctx.getTimeoutSeconds()
        );

        StreamCallback callback = streamCallbackFactory.create(onChunk, onComplete, onError);
        adapter.chatStream(request, callback);

        log.info("Stream request processed: model={}, provider={}, newArch={}",
            request.getModel(), ctx.getProviderName(), ctx.isNewArchitecture());
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
                    .cost(calculateCost(response))
                    .traceId(null)
                    .occurredOn(Instant.now())
                    .build();

            eventPublisher.publishEvent(event);
            log.debug("Published TokenUsedEvent for model={}", request.getModel());
        }
    }

    /**
     * 计算请求成本（简化版，不再依赖 RoutingContext.model）
     */
    private BigDecimal calculateCost(LLMResponse response) {
        // 成本计算已由路由层或用量记录层处理
        return BigDecimal.ZERO;
    }
}