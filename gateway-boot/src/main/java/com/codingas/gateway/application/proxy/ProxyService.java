package com.codingas.gateway.application.proxy;

import com.codingas.gateway.application.proxy.dto.LLMRequest;
import com.codingas.gateway.application.proxy.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RoutingStrategy;
import com.codingas.gateway.domain.security.service.UserAuthResult;

import java.util.function.Consumer;

/**
 * 代理服务接口
 *
 * <p>Application 层统一入口，编排代理请求处理流程。</p>
 * <p>支持双路路由：新架构（UserApiKey → Product）和旧架构（GatewayApiKey → Provider）。</p>
 */
public interface ProxyService {

    /**
     * 代理转发非流式请求（带认证结果，支持双路路由）
     *
     * @param request LLM 请求
     * @param authResult 认证结果
     * @param strategy 路由策略
     * @return LLM 响应
     */
    LLMResponse proxy(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy);

    /**
     * 代理转发非流式请求（旧接口，降级兼容）
     */
    LLMResponse proxy(LLMRequest request, RoutingStrategy strategy);

    /**
     * 代理转发流式请求（带认证结果）
     */
    void proxyStream(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy, Consumer<String> onChunk);

    /**
     * 代理转发流式请求（旧接口）
     */
    void proxyStream(LLMRequest request, RoutingStrategy strategy, Consumer<String> onChunk);

    /**
     * 代理转发流式请求（带完成和错误回调，旧接口）
     */
    void proxyStream(LLMRequest request, RoutingStrategy strategy,
                     Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError);

    /**
     * 代理转发流式请求（带完成和错误回调，带认证结果）
     */
    void proxyStream(LLMRequest request, UserAuthResult authResult, RoutingStrategy strategy,
                     Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError);
}
