package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 适配器加载器
 *
 * <p>使用 Java SPI 机制加载所有 LLMProviderAdapter 实现类。</p>
 */
@Slf4j
public class AdapterLoader {

    /**
     * 加载所有可用的适配器
     *
     * @return 适配器列表
     */
    public static List<LLMProviderAdapter> loadAdapters() {
        List<LLMProviderAdapter> adapters = new ArrayList<>();
        ServiceLoader<LLMProviderAdapter> loader = ServiceLoader.load(LLMProviderAdapter.class);

        for (LLMProviderAdapter adapter : loader) {
            log.info("Discovered LLM provider adapter: {} (type={})",
                    adapter.getProviderCode(),
                    adapter.getProviderType());
            adapters.add(adapter);
        }

        log.info("Total adapters loaded: {}", adapters.size());
        return adapters;
    }
}
