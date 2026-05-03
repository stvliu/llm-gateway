package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.Provider;

import java.util.List;
import java.util.Optional;

/**
 * 提供商网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ProviderGateway {

    /**
     * 保存提供商
     *
     * @param provider 提供商实体
     * @return 保存后的实体
     */
    Provider save(Provider provider);

    /**
     * 根据 ID 查找提供商
     *
     * @param id 提供商 ID
     * @return 提供商信息，不存在返回空
     */
    Optional<Provider> findById(Long id);

    /**
     * 根据提供商代码查找提供商
     *
     * @param providerCode 提供商代码
     * @return 提供商信息，不存在返回空
     */
    Optional<Provider> findByProviderCode(String providerCode);

    /**
     * 查询所有提供商
     *
     * @return 提供商列表
     */
    List<Provider> findAll();

    /**
     * 查找所有活跃提供商
     *
     * @return 活跃提供商列表
     */
    List<Provider> findAllActive();

    /**
     * 根据状态查找提供商
     *
     * @param status 提供商状态
     * @return 提供商列表
     */
    List<Provider> findByStatus(Provider.ProviderStatus status);

    /**
     * 统计提供商总数
     *
     * @return 提供商数量
     */
    long count();

    /**
     * 删除提供商
     *
     * @param provider 提供商实体
     */
    void delete(Provider provider);

    /**
     * 检查提供商代码是否存在
     *
     * @param providerCode 提供商代码
     * @return 是否存在
     */
    boolean existsByProviderCode(String providerCode);
}
