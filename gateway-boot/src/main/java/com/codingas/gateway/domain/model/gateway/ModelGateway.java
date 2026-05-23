package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.Model;

import java.util.List;
import java.util.Optional;

/**
 * 模型网关接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelGateway {

    /**
     * 保存模型
     *
     * @param model 模型实体
     * @return 保存后的实体
     */
    Model save(Model model);

    /**
     * 根据 ID 查找模型
     *
     * @param id 模型 ID
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findById(Long id);

    /**
     * 根据提供商模型 ID 查找模型
     *
     * @param providerModelId 提供商模型 ID（如 "gpt-4"）
     * @return 模型信息，不存在返回空
     */
    Optional<Model> findByProviderModelId(String providerModelId);

    /**
     * 查找同名模型的所有活跃渠道
     *
     * <p>用于多供应商路由，返回所有 provider_model_id 匹配且状态为 ACTIVE 的模型。</p>
     *
     * @param providerModelId 提供商模型 ID
     * @return 活跃渠道列表，按 priority 升序排序
     */
    List<Model> findActiveByProviderModelId(String providerModelId);

    /**
     * 查找同名模型的所有记录
     *
     * <p>用于管理和展示，返回所有 provider_model_id 匹配的模型（不限状态）。</p>
     *
     * @param providerModelId 提供商模型 ID
     * @return 所有渠道列表，按 priority 升序排序
     */
    List<Model> findAllByProviderModelId(String providerModelId);

    /**
     * 查询所有模型
     *
     * @return 模型列表
     */
    List<Model> findAll();

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

    List<Model> findByIds(List<Long> ids);

    /**
     * 统计模型总数
     *
     * @return 模型数量
     */
    long count();

    /**
     * 删除模型
     *
     * @param model 模型实体
     */
    void delete(Model model);

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
