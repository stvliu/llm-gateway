package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 适配器注册表
 *
 * <p>管理所有注册的 LLM 适配器，按提供商名称索引。</p>
 * <p>旧架构兼容组件，新架构下由 ProtocolGateway 体系替代。</p>
 */
@Slf4j
@Component
public class AdapterRegistry {

    private final Map<String, LLMAdapter> adaptersByName = new HashMap<>();
    private final Map<String, LLMAdapter> adaptersByCode = new HashMap<>();

    /**
     * 注册单个适配器
     */
    public void register(LLMAdapter adapter) {
        adaptersByName.put(adapter.getProviderName(), adapter);
        adaptersByCode.put(adapter.getProviderCode(), adapter);
        log.debug("Registered adapter: {} -> {}",
                adapter.getProviderCode(),
                adapter.getProviderName());
    }

    /**
     * 批量注册适配器
     */
    public void registerAll(List<LLMAdapter> adapters) {
        for (LLMAdapter adapter : adapters) {
            register(adapter);
        }
        log.info("Registered {} adapters", adapters.size());
    }

    /**
     * 根据 ProviderCode 获取适配器
     */
    public Optional<LLMAdapter> getAdapter(String providerCode) {
        return Optional.ofNullable(adaptersByCode.get(providerCode));
    }

    /**
     * 根据供应商名称获取适配器
     */
    public Optional<LLMAdapter> getAdapterByName(String providerName) {
        return Optional.ofNullable(adaptersByName.get(providerName));
    }

    /**
     * 获取所有已注册的适配器
     */
    public List<LLMAdapter> getAllAdapters() {
        return List.copyOf(adaptersByCode.values());
    }

    /**
     * 检查是否有指定名称的适配器
     */
    public boolean hasAdapterByName(String providerName) {
        return adaptersByName.containsKey(providerName);
    }

    /**
     * 检查是否有指定编码的适配器
     */
    public boolean hasAdapterByCode(String providerCode) {
        return adaptersByCode.containsKey(providerCode);
    }
}