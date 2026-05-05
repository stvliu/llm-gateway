package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMGateway;
import com.codingas.gateway.domain.proxy.gateway.LLMGatewayRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 适配器注册表
 *
 * <p>管理所有注册的 LLM 适配器，按提供商类型索引。</p>
 * <p>实现 Domain 层的 LLMGatewayRegistry 接口，提供技术防腐能力。</p>
 */
@Slf4j
@Component
public class AdapterRegistry implements LLMGatewayRegistry {

    private final Map<ProviderType, LLMAdapter> adaptersByType = new EnumMap<>(ProviderType.class);
    private final Map<String, LLMAdapter> adaptersByCode = new java.util.HashMap<>();

    /**
     * 注册单个适配器
     *
     * @param adapter 适配器实例
     */
    public void register(LLMAdapter adapter) {
        adaptersByType.put(adapter.getProviderType(), adapter);
        adaptersByCode.put(adapter.getProviderCode(), adapter);
        log.debug("Registered adapter: {} -> {}",
                adapter.getProviderCode(),
                adapter.getProviderType());
    }

    /**
     * 批量注册适配器
     *
     * @param adapters 适配器列表
     */
    public void registerAll(List<LLMAdapter> adapters) {
        for (LLMAdapter adapter : adapters) {
            register(adapter);
        }
        log.info("Registered {} adapters", adapters.size());
    }

    /**
     * 根据 ProviderCode 获取适配器
     *
     * @param providerCode 提供商编码
     * @return 适配器实例
     */
    public Optional<LLMAdapter> getAdapter(String providerCode) {
        return Optional.ofNullable(adaptersByCode.get(providerCode));
    }

    /**
     * 获取所有已注册的适配器
     *
     * @return 适配器列表
     */
    public List<LLMAdapter> getAllAdapters() {
        return List.copyOf(adaptersByCode.values());
    }

    /**
     * 检查是否有指定类型的适配器
     *
     * @param providerType 提供商类型
     * @return true 如果存在
     */
    public boolean hasAdapter(ProviderType providerType) {
        return adaptersByType.containsKey(providerType);
    }

    /**
     * 检查是否有指定编码的适配器
     *
     * @param providerCode 提供商编码
     * @return true 如果存在
     */
    public boolean hasAdapter(String providerCode) {
        return adaptersByCode.containsKey(providerCode);
    }

    // ==================== 实现 LLMGatewayRegistry 接口 ====================

    @Override
    public Optional<LLMGateway> getGateway(ProviderType providerType) {
        return Optional.ofNullable(adaptersByType.get(providerType))
                .map(adapter -> (LLMGateway) adapter);
    }

    @Override
    public Iterable<LLMGateway> getAllGateways() {
        return () -> adaptersByCode.values().stream()
                .map(adapter -> (LLMGateway) adapter)
                .iterator();
    }
}
