package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.Provider;

import java.util.List;
import java.util.Optional;

/**
 * 提供商网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作提供商。</p>
 */
public interface ProviderGateway {

    /**
     * 根据提供商编码查找提供商
     *
     * @param providerCode 提供商编码
     * @return 提供商信息，不存在返回空
     */
    Optional<Provider> findByProviderCode(String providerCode);

    /**
     * 根据提供商 ID 查找提供商
     *
     * @param providerId 提供商 ID
     * @return 提供商信息，不存在返回空
     */
    Optional<Provider> findById(Long providerId);

    /**
     * 查找所有活跃提供商
     *
     * @return 活跃提供商列表
     */
    List<Provider> findAllActive();

    /**
     * 保存提供商
     *
     * @param provider 提供商实体
     * @return 保存后的实体
     */
    Provider save(Provider provider);
}
