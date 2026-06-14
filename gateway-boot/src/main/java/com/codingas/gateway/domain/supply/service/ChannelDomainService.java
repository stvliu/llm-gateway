package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.exception.ChannelException;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
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
    private final ChannelEndpointGateway channelEndpointGateway;

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
     * 暂停渠道
     */
    public Channel disable(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channel.setState(ChannelState.SUSPENDED);
        return channelGateway.save(channel);
    }

    /**
     * 软删除渠道
     */
    public void delete(Long id) {
        Channel channel = channelGateway.findById(id)
                .orElseThrow(() -> new ChannelException("CHANNEL_NOT_FOUND", "渠道不存在: " + id));
        channelGateway.deleteById(id);
    }

    /**
     * 查找指定供应商的活跃渠道
     */
    public List<Channel> findActiveByProviderId(Long providerId) {
        return channelGateway.findByProviderId(providerId).stream()
                .filter(Channel::isAvailable)
                .toList();
    }

    /**
     * 根据入站协议解析渠道端点
     *
     * <p>优先匹配同名协议端点，无匹配则降级选第一个可用端点。</p>
     *
     * @param channel 渠道实体
     * @param inboundProtocol 入站请求的协议类型
     * @return 匹配的 ChannelEndpoint
     * @throws ChannelException 渠道无可用端点
     */
    public ChannelEndpoint resolveEndpoint(Channel channel, Protocol inboundProtocol) {
        List<ChannelEndpoint> activeEndpoints = channelEndpointGateway.findByChannelId(channel.getId());

        if (activeEndpoints.isEmpty()) {
            throw new ChannelException("CHANNEL_NO_ENDPOINT",
                    "渠道无可用端点: channelId=" + channel.getId());
        }

        // 优先匹配同名协议端点
        Optional<ChannelEndpoint> matched = activeEndpoints.stream()
                .filter(e -> e.getProtocol() == inboundProtocol)
                .findFirst();

        if (matched.isPresent()) {
            log.debug("Endpoint protocol matched: channelId={}, endpointId={}, protocol={}",
                    channel.getId(), matched.get().getId(), inboundProtocol);
            return matched.get();
        }

        // 降级：选第一个可用端点
        ChannelEndpoint fallback = activeEndpoints.get(0);
        log.info("Endpoint protocol fallback: channelId={}, inbound={}, fallback={}",
                channel.getId(), inboundProtocol, fallback.getProtocol());
        return fallback;
    }
}