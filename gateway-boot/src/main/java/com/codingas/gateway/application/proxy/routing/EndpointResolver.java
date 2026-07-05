package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 端点解析器 — 根据 channelId 和协议查找可用端点
 */
@Component
@RequiredArgsConstructor
public class EndpointResolver {

    private final ChannelEndpointGateway channelEndpointGateway;

    /**
     * 根据 channelId 和入站协议解析可用的端点
     *
     * <p>优先匹配协议同源的端点，避免不必要的跨协议转换。
     * 如果找不到匹配协议的同源端点，回退到任意可用端点。</p>
     *
     * @param channelId 渠道 ID
     * @param protocol  入站协议
     * @return 可用的 ChannelEndpoint
     * @throws ResourceNotFoundException 未找到可用端点
     */
    public ChannelEndpoint resolve(Long channelId, Protocol protocol) {
        // 优先匹配协议同源的端点
        return channelEndpointGateway.findByChannelIdAndProtocol(channelId, protocol)
                .orElseGet(() -> {
                    List<ChannelEndpoint> endpoints = channelEndpointGateway.findByChannelId(channelId);
                    return endpoints.stream()
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("ChannelEndpoint", channelId));
                });
    }
}
