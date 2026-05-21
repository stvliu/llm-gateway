package com.codingas.gateway.domain.proxy.gateway;

import java.util.Optional;

/**
 * LLM 网关注册表接口
 *
 * <p>按供应商名称查找 LLM 网关适配器。</p>
 */
public interface LLMGatewayRegistry {

    /**
     * 根据供应商名称获取网关
     *
     * @param providerName 供应商名称
     * @return 网关，如果不存在则返回空
     */
    Optional<LLMGateway> getGateway(String providerName);

    /**
     * 获取所有已注册的网关
     *
     * @return 所有网关
     */
    Iterable<LLMGateway> getAllGateways();
}