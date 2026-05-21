package com.codingas.gateway.infrastructure.proxy.gateway.rpc;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 适配器加载器
 *
 * <p>使用 Java SPI 机制加载所有 LLMAdapter 实现类。</p>
 */
@Slf4j
public class AdapterLoader {

    /**
     * 加载所有可用的适配器
     *
     * @return 适配器列表
     */
    public static List<LLMAdapter> loadAdapters() {
        List<LLMAdapter> adapters = new ArrayList<>();
        ServiceLoader<LLMAdapter> loader = ServiceLoader.load(LLMAdapter.class);

        for (LLMAdapter adapter : loader) {
            log.info("Discovered LLM adapter: {} (type={})",
                    adapter.getProviderCode(),
                    adapter.getProviderName());
            adapters.add(adapter);
        }

        log.info("Total adapters loaded: {}", adapters.size());
        return adapters;
    }
}
