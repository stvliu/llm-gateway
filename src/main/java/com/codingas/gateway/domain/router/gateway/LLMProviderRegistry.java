package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.common.enums.ProviderType;

import java.util.Optional;

/**
 * LLM 提供商注册表接口
 *
 * <p>Domain 层定义的接口，用于获取指定类型的 LLM 提供商适配器。</p>
 */
public interface LLMProviderRegistry {

    /**
     * 根据提供商类型获取适配器
     *
     * @param providerType 提供商类型
     * @return 适配器，如果不存在则返回空
     */
    Optional<LLMProviderPort> getAdapter(ProviderType providerType);

    /**
     * 获取所有已注册的适配器
     *
     * @return 所有适配器
     */
    Iterable<LLMProviderPort> getAllAdapters();
}
