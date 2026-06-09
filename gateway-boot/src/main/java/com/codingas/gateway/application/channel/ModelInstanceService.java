package com.codingas.gateway.application.channel;

import com.codingas.gateway.application.channel.dto.ModelInstanceCreateRequest;
import com.codingas.gateway.application.channel.dto.ModelInstanceResponse;

import java.util.List;

/**
 * 模型实例应用服务接口
 */
public interface ModelInstanceService {

    /**
     * 查询指定渠道下的所有模型实例
     */
    List<ModelInstanceResponse> getInstancesByChannelId(Long channelId);

    /**
     * 创建模型实例
     */
    ModelInstanceResponse create(ModelInstanceCreateRequest request);

    /**
     * 删除模型实例
     */
    void delete(Long channelId, Long id);

    /**
     * 启用/禁用模型实例
     */
    void setEnabled(Long channelId, Long id, boolean enabled);

    /**
     * 更新模型实例的上游模型名
     *
     * @param upstreamModelName 新的上游模型名，null 表示走默认（= Model.modelName）
     */
    void updateUpstreamModelName(Long channelId, Long id, String upstreamModelName);
}