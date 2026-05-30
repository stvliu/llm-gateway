package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ChannelModel;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 路由解析门面 — 编排四个子组件，组装 RoutingContext
 */
@Component
@RequiredArgsConstructor
public class RoutingResolver {

    private final ModelMatcher modelMatcher;
    private final ChannelSelector channelSelector;
    private final CredentialResolver credentialResolver;
    private final EndpointResolver endpointResolver;
    private final ChannelGateway channelGateway;

    /**
     * 根据模型名称解析完整的路由上下文
     *
     * @param modelName 模型名称
     * @param protocol  入站协议
     * @return 路由上下文
     */
    public RoutingContext resolve(String modelName, Protocol protocol) {
        // 1. 模型匹配
        Model model = modelMatcher.match(modelName);

        // 2. 通道选择
        ChannelModel channelModel = channelSelector.select(model.getId());

        // 3. 凭证解析
        String apiKey = credentialResolver.resolve(channelModel.getChannelId());

        // 4. 端点解析（优先匹配协议同源）
        ChannelEndpoint endpoint = endpointResolver.resolve(channelModel.getChannelId(), protocol);

        // 5. 通道信息
        Channel channel = channelGateway.findById(channelModel.getChannelId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel", channelModel.getChannelId()));

        // 6. 判断是否需要协议适配
        boolean needsAdaptation = endpoint.getProtocol() != protocol;

        // 7. 组装 RoutingContext
        return new RoutingContext(
                channel.getId(),
                endpoint.getId(),
                endpoint.getEndpointUrl(),
                endpoint.getProtocol(),
                apiKey,
                channel.getTimeout(),
                needsAdaptation,
                model.getModelName(),
                channelModel.getUpstreamModelName()
        );
    }
}
