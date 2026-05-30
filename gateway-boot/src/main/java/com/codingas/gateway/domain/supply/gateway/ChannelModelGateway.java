package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.enums.ChannelModelState;

import java.util.List;
import java.util.Optional;

/**
 * 渠道模型仓储接口
 */
public interface ChannelModelGateway {

    /**
     * 保存渠道模型
     */
    ChannelModel save(ChannelModel channelModel);

    /**
     * 根据ID查找
     */
    Optional<ChannelModel> findById(Long id);

    /**
     * 根据渠道ID查找所有渠道模型
     */
    List<ChannelModel> findByChannelId(Long channelId);

    /**
     * 根据渠道ID查找活跃的渠道模型
     */
    List<ChannelModel> findActiveByChannelId(Long channelId);

    /**
     * 根据模型规格ID查找活跃的渠道模型
     */
    List<ChannelModel> findActiveByModelId(Long modelId);

    /**
     * 根据渠道ID和状态查找
     */
    List<ChannelModel> findByChannelIdAndState(Long channelId, ChannelModelState state);

    /**
     * 根据ID列表批量查找
     */
    List<ChannelModel> findByIds(List<Long> ids);

    /**
     * 检查渠道模型关联是否已存在
     */
    boolean existsByChannelIdAndModelId(Long channelId, Long modelId);

    /**
     * 批量保存渠道模型关联
     */
    List<ChannelModel> saveAll(List<ChannelModel> channelModels);

    /**
     * 删除渠道模型
     */
    void deleteById(Long id);
}
