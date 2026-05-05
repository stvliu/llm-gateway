package com.codingas.gateway.domain.proxy.gateway;

import com.codingas.gateway.common.enums.ProviderType;

import java.util.Optional;

/**
 * LLM 网关注册表接口
 *
 * <p>技术防腐层，隔离适配器注册与查找逻辑。</p>
 * <p>Domain 层定义的接口，用于获取指定类型的 LLM 网关适配器。</p>
 */
public interface LLMGatewayRegistry {

    /**
     * 根据提供商类型获取网关
     *
     * @param providerType 提供商类型
     * @return 网关，如果不存在则返回空
     */
    Optional<LLMGateway> getGateway(ProviderType providerType);

    /**
     * 获取所有已注册的网关
     *
     * @return 所有网关
     */
    Iterable<LLMGateway> getAllGateways();
}
