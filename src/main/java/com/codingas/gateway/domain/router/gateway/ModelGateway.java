package com.codingas.gateway.domain.router.gateway;

import com.codingas.gateway.domain.router.entity.Model;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelGateway {

    /**
     * 根据 ID 查找模型
     *
     * @param id 模型 ID
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findById(Long id);

    /**
     * 根据模型代码查找模型
     *
     * @param modelCode 模型代码
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findByModelCode(String modelCode);

    /**
     * 查找所有活跃模型
     *
     * @return 活跃模型列表
     */
    List<Model> findAllActive();

    /**
     * 根据提供商 ID 查找模型
     *
     * @param providerId 提供商 ID
     * @return 模型列表
     */
    List<Model> findByProviderId(Long providerId);

    /**
     * 保存模型
     *
     * @param model 模型实体
     * @return 保存后的实体
     */
    Model save(Model model);
}
