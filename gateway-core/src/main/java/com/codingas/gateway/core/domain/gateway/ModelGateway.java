package com.codingas.gateway.core.domain.gateway;

import com.codingas.gateway.core.domain.entity.Model;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 * <p>Domain 不直接依赖持久化，通过此接口操作模型。</p>
 */
public interface ModelGateway {

    /**
     * 根据模型编码查找模型
     *
     * @param modelCode 模型编码
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findByModelCode(String modelCode);

    /**
     * 根据提供商编码查找所有模型
     *
     * @param providerCode 提供商编码
     * @return 模型列表
     */
    List<Model> findByProviderCode(String providerCode);

    /**
     * 根据路由分组 ID 查找所有模型
     *
     * @param routeGroupId 路由分组 ID
     * @return 模型列表
     */
    List<Model> findByRouteGroupId(Long routeGroupId);

    /**
     * 根据模型 ID 查找模型
     *
     * @param modelId 模型 ID
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findById(Long modelId);

    /**
     * 保存模型
     *
     * @param model 模型实体
     * @return 保存后的实体
     */
    Model save(Model model);

    /**
     * 查找所有活跃模型
     *
     * @return 活跃模型列表
     */
    List<Model> findAllActive();
}
