package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 端点解析器 — 根据 channelId 查找可用端点
 */
@Component
@RequiredArgsConstructor
public class EndpointResolver {

    private final ChannelEndpointGateway channelEndpointGateway;

    /**
     * 根据 channelId 解析可用的端点
     *
     * <p>查找渠道下所有端点，返回第一个可用端点。</p>
     *
     * @param channelId 通道 ID
     * @return 可用的 ChannelEndpoint
     * @throws ResourceNotFoundException 未找到可用端点
     */
    public ChannelEndpoint resolve(Long channelId) {
        List<ChannelEndpoint> endpoints = channelEndpointGateway.findByChannelId(channelId);
        return endpoints.stream()
                .filter(ChannelEndpoint::isAvailable)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("ChannelEndpoint", channelId));
    }
}
