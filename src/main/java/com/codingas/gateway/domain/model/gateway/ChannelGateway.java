package com.codingas.gateway.domain.model.gateway;

import com.codingas.gateway.domain.model.entity.Channel;

import java.util.List;
import java.util.Optional;

/**
 * 渠道网关接口
 */
public interface ChannelGateway {

    /**
     * 根据 ID 查找渠道
     */
    Optional<Channel> findById(Long id);

    /**
     * 根据渠道编码查找渠道
     */
    Optional<Channel> findByChannelCode(String channelCode);

    /**
     * 根据提供商 ID 查找所有渠道
     */
    List<Channel> findByProviderId(Long providerId);

    /**
     * 查找所有活跃渠道
     */
    List<Channel> findAllActive();

    /**
     * 保存渠道
     */
    Channel save(Channel channel);
}
