package com.codingas.gateway.application.proxy;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.common.dto.LLMResponse;
import com.codingas.gateway.domain.proxy.entity.RouteGroup;

import java.util.function.Consumer;

/**
 * 代理服务接口
 *
 * <p>Application 层统一入口，编排代理请求处理流程。</p>
 *
 * <p>处理流程（按架构定义）：</p>
 * <ol>
 *   <li>认证检查</li>
 *   <li>限额检查</li>
 *   <li>语义缓存检查（可选）</li>
 *   <li>路由选择</li>
 *   <li>降级检查（可选）</li>
 *   <li>代理转发</li>
 *   <li>记录用量</li>
 *   <li>审计日志</li>
 *   <li>异常告警（可选）</li>
 * </ol>
 */
public interface ProxyService {

    /**
     * 代理转发非流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return LLM 响应
     */
    LLMResponse proxy(LLMRequest request, RouteGroup.RoutingStrategy strategy);

    /**
     * 代理转发流式请求
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @param onChunk 流式响应回调
     */
    void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy, Consumer<String> onChunk);

    /**
     * 代理转发流式请求（带完成和错误回调）
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @param onChunk 流式响应回调
     * @param onComplete 完成回调
     * @param onError 错误回调
     */
    void proxyStream(LLMRequest request, RouteGroup.RoutingStrategy strategy,
                     Consumer<String> onChunk, Runnable onComplete, Consumer<Throwable> onError);
}
