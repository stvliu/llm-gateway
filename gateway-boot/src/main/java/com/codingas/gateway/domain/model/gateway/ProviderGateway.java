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
     * 获取最大版本号
     *
     * <p>用于变更检测。</p>
     *
     * @return 最大版本号，无数据返回 0
     */
    default long getMaxVersion() {
        return 0L;
    }

    /**
     * 根据名称查找提供商
     *
     * @param name 提供商名称
     * @return 提供商信息，不存在返回空
     */
    Optional<Provider> findByName(String name);

    /**
     * 检查名称是否已存在
     *
     * @param name 提供商名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
