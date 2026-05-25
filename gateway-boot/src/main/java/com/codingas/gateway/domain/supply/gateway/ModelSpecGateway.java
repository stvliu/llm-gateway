package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ModelSpec;

import java.util.List;
import java.util.Optional;

/**
 * 模型规格持久化接口
 *
 * <p>定义在 domain 层，由 infrastructure 层实现。</p>
 */
public interface ModelSpecGateway {

    /**
     * 保存模型规格
     */
    ModelSpec save(ModelSpec modelSpec);

    /**
     * 根据 ID 查找模型规格
     */
    Optional<ModelSpec> findById(Long id);

    /**
     * 根据供应商侧模型 ID 查找模型规格
     */
    Optional<ModelSpec> findByProviderModelId(String providerModelId);

    /**
     * 查找同名模型规格的所有活跃渠道
     */
    List<ModelSpec> findActiveByProviderModelId(String providerModelId);

    /**
     * 查询所有模型规格
     */
    List<ModelSpec> findAll();

    /**
     * 查找所有活跃模型规格
     */
    List<ModelSpec> findAllActive();

    /**
     * 根据供应商 ID 查找模型规格
     *
     * @deprecated providerId 已从 ModelSpec 移除，后续通过 Supply 实体关联查询
     */
    @Deprecated
    List<ModelSpec> findByProviderId(Long providerId);

    /**
     * 批量查找模型规格
     */
    List<ModelSpec> findByIds(List<Long> ids);

    /**
     * 统计模型规格总数
     */
    long count();

    /**
     * 删除模型规格
     */
    void delete(ModelSpec modelSpec);

    /**
     * 获取最大版本号
     */
    default long getMaxVersion() {
        return 0L;
    }
}