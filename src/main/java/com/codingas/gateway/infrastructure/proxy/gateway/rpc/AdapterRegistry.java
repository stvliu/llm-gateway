package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import com.codingas.gateway.common.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 适配器注册表
 *
 * <p>管理所有注册的 LLM 适配器，按提供商类型索引。</p>
 */
@Slf4j
public class AdapterRegistry {

    private final Map<ProviderType, LLMProviderAdapter> adaptersByType = new EnumMap<>(ProviderType.class);
    private final Map<String, LLMProviderAdapter> adaptersByCode = new java.util.HashMap<>();

    /**
     * 注册单个适配器
     *
     * @param adapter 适配器实例
     */
    public void register(LLMProviderAdapter adapter) {
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
    public void registerAll(List<LLMProviderAdapter> adapters) {
        for (LLMProviderAdapter adapter : adapters) {
            register(adapter);
        }
        log.info("Registered {} adapters", adapters.size());
    }

    /**
     * 根据 ProviderType 获取适配器
     *
     * @param providerType 提供商类型
     * @return 适配器实例
     */
    public Optional<LLMProviderAdapter> getAdapter(ProviderType providerType) {
        return Optional.ofNullable(adaptersByType.get(providerType));
    }

    /**
     * 根据 ProviderCode 获取适配器
     *
     * @param providerCode 提供商编码
     * @return 适配器实例
     */
    public Optional<LLMProviderAdapter> getAdapter(String providerCode) {
        return Optional.ofNullable(adaptersByCode.get(providerCode));
    }

    /**
     * 获取所有已注册的适配器
     *
     * @return 适配器列表
     */
    public List<LLMProviderAdapter> getAllAdapters() {
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
}
