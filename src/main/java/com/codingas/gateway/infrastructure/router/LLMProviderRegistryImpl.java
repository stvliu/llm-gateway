package com.codingas.gateway.infrastructure.gateway.router;

import com.codingas.gateway.common.enums.ProviderType;
import com.codingas.gateway.domain.proxy.gateway.LLMProviderPort;
import com.codingas.gateway.domain.proxy.gateway.LLMProviderRegistry;
import com.codingas.gateway.infrastructure.adapter.LLMProviderAdapter;
import com.codingas.gateway.infrastructure.spi.AdapterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM 提供商注册表实现
 *
 * <p>将 AdapterRegistry 适配为 Domain 层接口。</p>
 */
@Component
@RequiredArgsConstructor
public class LLMProviderRegistryImpl implements LLMProviderRegistry {

    private final AdapterRegistry adapterRegistry;

    @Override
    public Iterable<LLMProviderPort> getAllAdapters() {
        List<LLMProviderAdapter> adapters = adapterRegistry.getAllAdapters();
        return () -> adapters.stream()
                .map(adapter -> (LLMProviderPort) adapter)
                .iterator();
    }

    @Override
    public java.util.Optional<LLMProviderPort> getAdapter(ProviderType providerType) {
        return adapterRegistry.getAdapter(providerType)
                .map(adapter -> (LLMProviderPort) adapter);
    }
}
