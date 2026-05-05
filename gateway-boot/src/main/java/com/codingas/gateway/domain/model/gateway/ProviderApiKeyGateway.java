package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.ProviderApiKey;

import java.util.Optional;

/**
 * 提供商 API 密钥网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作提供商 API 密钥。</p>
 */
public interface ProviderApiKeyGateway {

    /**
     * 根据密钥 ID 查找 API 密钥
     *
     * @param id 密钥 ID
     * @return API 密钥信息，不存在返回空
     */
    Optional<ProviderApiKey> findById(Long id);

    /**
     * 根据提供商 ID 查找 API 密钥
     *
     * @param providerId 提供商 ID
     * @return API 密钥信息，不存在返回空
     */
    Optional<ProviderApiKey> findByProviderId(Long providerId);

    /**
     * 保存提供商 API 密钥
     *
     * @param providerApiKey API 密钥实体
     * @return 保存后的实体
     */
    ProviderApiKey save(ProviderApiKey providerApiKey);

    /**
     * 获取最大版本号
     *
     * <p>用于变更检测。</p>
     *
     * @return 最大版本号，无数据返回 0
     */
    default long getMaxVersion() {
        return 0L;
    }
}
