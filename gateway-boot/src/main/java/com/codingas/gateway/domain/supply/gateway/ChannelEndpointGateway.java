package com.codingas.gateway.domain.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.Protocol;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点持久化接口
 */
public interface ChannelEndpointGateway {

    /**
     * 保存端点
     */
    ChannelEndpoint save(ChannelEndpoint endpoint);

    /**
     * 根据 ID 查找端点
     */
    Optional<ChannelEndpoint> findById(Long id);

    /**
     * 根据渠道 ID 查找所有端点
     */
    List<ChannelEndpoint> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 和协议查找端点
     */
    Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol);

    /**
     * 查询所有端点
     */
    List<ChannelEndpoint> findAll();

    /**
     * 删除端点
     */
    void deleteById(Long id);
}
