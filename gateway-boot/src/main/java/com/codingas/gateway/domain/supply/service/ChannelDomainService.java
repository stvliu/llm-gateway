package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 渠道领域服务
 *
 * <p>封装渠道相关的核心业务逻辑，替代原 ProductDomainService + ProductRoutingService。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelDomainService {

    private final ChannelGateway channelGateway;

    /**
     * 创建渠道
     *
     * @throws ChannelException 同一供应商下渠道名重复
     */
    public Channel create(Channel channel) {
        if (channelGateway.existsByProviderIdAndName(channel.getProviderId(), channel.getName())) {
            throw new ChannelException("CHANNEL_NAME_DUPLICATE", "同一供应商下渠道名已存在: " + channel.getName());
        }
        return channelGateway.save(channel);
    }

    /**
     * 更新渠道
     */
    public Channel update(Channel channel) {
        return channelGateway.save(channel);
    }

    /**
     * 启用渠道
     */
    public Channel enable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.ACTIVE);
        return channelGateway.save(channel);
    }

    /**
     * 禁用渠道
     */
    public Channel disable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.DISABLED);
        return channelGateway.save(channel);
    }

    /**
     * 软删除渠道
     */
    public void delete(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.DELETED);
        channelGateway.save(channel);
    }

    /**
     * 查找路由上下文
     */
    public Optional<RoutingContext> findRoutingContext(Long channelId) {
        return channelGateway.findRoutingContext(channelId);
    }

    /**
     * 查找指定供应商的活跃渠道
     */
    public List<Channel> findActiveByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId).stream()
                .filter(Channel::isAvailable)
                .toList();
    }
}