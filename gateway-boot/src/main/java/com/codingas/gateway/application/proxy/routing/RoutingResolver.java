package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
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
    private final InstanceSelector instanceSelector;
    private final CredentialResolver credentialResolver;
    private final EndpointResolver endpointResolver;
    private final ChannelGateway channelGateway;

    /**
     * 根据模型名称解析完整的路由上下文
     *
     * @param modelName 模型名称
     * @param protocol  入站协议
     * @param userId    用户 ID（用于团队渠道权限过滤）
     * @return 路由上下文
     */
    public RoutingContext resolve(String modelName, Protocol protocol, Long userId) {
        // 1. 模型匹配
        Model model = modelMatcher.match(modelName);

        // 2. 实例选择（按 priority 排序）
        ModelInstance modelInstance = instanceSelector.select(model.getId(), userId);

        // 3. 凭证解析
        String apiKey = credentialResolver.resolve(modelInstance.getChannelId());

        // 4. 端点解析（优先匹配协议同源）
        ChannelEndpoint endpoint = endpointResolver.resolve(modelInstance.getChannelId(), protocol);

        // 5. 通道信息
        Channel channel = channelGateway.findById(modelInstance.getChannelId())
                .orElseThrow(() -> new ResourceNotFoundException("Channel", modelInstance.getChannelId()));

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
                modelInstance.getUpstreamModelName()
        );
    }
}
