package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ChannelModelCreateRequest;
import com.codingas.gateway.application.channel.dto.ChannelModelResponse;

import java.util.List;

/**
 * 渠道模型关联应用服务接口
 */
public interface ChannelModelService {

    /**
     * 查询指定渠道下的所有模型关联
     */
    List<ChannelModelResponse> getModelsByChannelId(Long channelId);

    /**
     * 创建渠道模型关联
     */
    ChannelModelResponse create(Long channelId, ChannelModelCreateRequest request);

    /**
     * 删除渠道模型关联
     */
    void delete(Long channelId, Long id);

    /**
     * 启用/禁用渠道模型关联
     */
    void setEnabled(Long channelId, Long id, boolean enabled);
}