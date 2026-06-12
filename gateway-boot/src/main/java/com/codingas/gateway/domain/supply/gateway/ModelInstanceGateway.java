package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ModelInstance;

import java.util.List;
import java.util.Optional;

/**
 * 模型实例仓储接口
 */
public interface ModelInstanceGateway {

    /**
     * 保存模型实例
     */
    ModelInstance save(ModelInstance instance);

    /**
     * 根据ID查找
     */
    Optional<ModelInstance> findById(Long id);

    /**
     * 根据渠道ID查找所有模型实例
     */
    List<ModelInstance> findByChannelId(Long channelId);

    /**
     * 根据渠道ID查找活跃的模型实例
     */
    List<ModelInstance> findActiveByChannelId(Long channelId);

    /**
     * 根据模型规格ID查找活跃的模型实例
     */
    List<ModelInstance> findActiveByModelId(Long modelId);

    /**
     * 根据模型规格ID查找活跃的模型实例（按优先级排序）
     */
    List<ModelInstance> findActiveByModelIdOrderByPriority(Long modelId);

    /**
     * 根据渠道ID和状态查找
     */
    List<ModelInstance> findByChannelIdAndState(Long channelId, String state);

    /**
     * 根据ID列表批量查找
     */
    List<ModelInstance> findByIds(List<Long> ids);

    /**
     * 检查渠道模型关联是否已存在
     */
    boolean existsByChannelIdAndModelId(Long channelId, Long modelId);

    /**
     * 批量保存模型实例
     */
    List<ModelInstance> saveAll(List<ModelInstance> instances);

    /**
     * 删除模型实例
     */
    void deleteById(Long id);
}