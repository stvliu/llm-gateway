package com.codingas.gateway.domain.router.service;

import com.codingas.gateway.common.dto.LLMRequest;
import com.codingas.gateway.domain.router.entity.Model;
import com.codingas.gateway.domain.router.entity.Provider;
import com.codingas.gateway.domain.router.entity.RouteGroup;
import com.codingas.gateway.domain.router.gateway.LLMProviderPort;
import com.codingas.gateway.domain.router.gateway.LLMProviderRegistry;
import com.codingas.gateway.domain.router.gateway.ModelGateway;
import com.codingas.gateway.domain.router.gateway.ModelRouter;
import com.codingas.gateway.domain.router.gateway.ProviderGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 默认模型路由器实现
 *
 * <p>基于 PRIORITY 策略选择最佳可用 Provider。</p>
 * <p>策略说明：</p>
 * <ul>
 *   <li>PRIORITY - 按 Model-RouteGroup-Provider 的优先级链选择</li>
 *   <li>ROUND_ROBIN - 按权重轮询（暂不支持）</li>
 *   <li>LEAST_LATENCY - 选择延迟最低的（暂不支持）</li>
 * </ul>
 *
 * <p>遵循 COLA 5.0 Gateway 模式：Domain 只依赖 Gateway 接口，不直接依赖服务。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultModelRouter implements ModelRouter {

    private final ModelGateway modelGateway;
    private final ProviderGateway providerGateway;
    private final LLMProviderRegistry providerRegistry;

    /**
     * 根据请求和策略选择最佳 Provider
     *
     * @param request LLM 请求
     * @param strategy 路由策略
     * @return 选中的 Provider 适配器
     * @throws NoSuchElementException 如果没有可用的 Provider
     */
    @Override
    public LLMProviderPort select(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        return select(request.getModel(), strategy);
    }

    /**
     * 根据模型代码和策略选择最佳 Provider
     *
     * @param modelCode 模型代码
     * @param strategy 路由策略
     * @return 选中的 Provider 适配器
     * @throws NoSuchElementException 如果没有可用的 Provider
     */
    @Override
    public LLMProviderPort select(String modelCode, RouteGroup.RoutingStrategy strategy) {
        // 1. 根据模型代码查找模型信息
        Model model = modelGateway.findByModelCode(modelCode)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelCode));

        // 2. 获取 Provider (通过 providerId)
        Provider provider = providerGateway.findById(model.getProviderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Provider not found: " + model.getProviderId()));

        // 3. 获取 Provider 类型并获取对应类型的适配器
        LLMProviderPort adapter = providerRegistry.getAdapter(provider.toProviderType())
                .orElseThrow(() -> new NoSuchElementException(
                        "No adapter available for provider type: " + provider.toProviderType()));

        // 4. 检查适配器是否可用
        if (!adapter.isAvailable()) {
            throw new NoSuchElementException(
                    "Adapter not available: " + adapter.getProviderCode());
        }

        log.info("Selected adapter: {} for model: {} (strategy: {})",
                adapter.getProviderCode(), modelCode, strategy);

        return adapter;
    }
}
