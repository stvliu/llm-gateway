package com.codingas.gateway.dispatch;

import com.codingas.gateway.adapter.LLMProviderAdapter;
import com.codingas.gateway.adapter.dto.LLMRequest;
import com.codingas.gateway.adapter.spi.AdapterRegistry;
import com.codingas.gateway.core.domain.entity.Model;
import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.entity.RouteGroup;
import com.codingas.gateway.core.service.ModelService;
import com.codingas.gateway.core.service.ProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultModelRouter implements ModelRouter {

    private final AdapterRegistry adapterRegistry;
    private final ModelService modelService;
    private final ProviderService providerService;

    @Override
    public LLMProviderAdapter select(LLMRequest request, RouteGroup.RoutingStrategy strategy) {
        return select(request.getModel(), strategy);
    }

    @Override
    public LLMProviderAdapter select(String modelCode, RouteGroup.RoutingStrategy strategy) {
        // 1. 根据模型代码查找模型信息
        Model model = modelService.findByModelCode(modelCode)
                .orElseThrow(() -> new NoSuchElementException("Model not found: " + modelCode));

        // 2. 获取 Provider (通过 providerId)
        Provider provider = providerService.findById(model.getProviderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Provider not found: " + model.getProviderId()));

        // 3. 获取 Provider 类型并转换为适配器类型
        String providerTypeCode = provider.getProviderType().name();

        // 4. 从适配器注册表获取对应类型的适配器
        LLMProviderAdapter adapter = adapterRegistry.getAdapter(providerTypeCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "No adapter available for provider type: " + providerTypeCode));

        // 5. 检查适配器是否可用
        if (!adapter.isAvailable()) {
            throw new NoSuchElementException(
                    "Adapter not available: " + adapter.getProviderCode());
        }

        log.info("Selected adapter: {} for model: {} (strategy: {})",
                adapter.getProviderCode(), modelCode, strategy);

        return adapter;
    }
}
