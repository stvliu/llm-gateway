package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.domain.router.entity.RouteGroup;

/**
 * 模型路由器接口
 *
 * <p>根据指定的路由策略选择一个合适的 LLM Provider。</p>
 */
public interface ModelRouter {

    /**
     * 根据请求和策略选择最佳 Provider
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return 选中的 Provider 适配器
     * @throws java.util.NoSuchElementException 如果没有可用的 Provider
     */
    LLMProviderPort select(LLMRequest request, RouteGroup.RoutingStrategy strategy);

    /**
     * 根据模型代码和策略选择最佳 Provider
     *
     * @param modelCode 模型代码
     * @param strategy 路由策略
     * @return 选中的 Provider 适配器
     * @throws java.util.NoSuchElementException 如果没有可用的 Provider
     */
    LLMProviderPort select(String modelCode, RouteGroup.RoutingStrategy strategy);
}
