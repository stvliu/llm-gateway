package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
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
     * @param modelName     模型名称
     * @param protocol      入站协议
     * @param applicationId 应用 ID（数据面权限锚点，透传至 InstanceSelector/RoutingRequest）
     * @param userId        用户 ID
     * @param role          用户角色（保留字段；数据面权限基于 applicationId 判定可见渠道）
     * @param strategy      路由策略
     * @return 路由上下文
     */
    public RoutingContext resolve(String modelName, Protocol protocol, Long applicationId, Long userId, String role, RoutingStrategy strategy) {
        // 1. 模型匹配
        Model model = modelMatcher.match(modelName);

        // 2. 实例选择（委托 RouterChain，传入 applicationId 作为权限锚点、protocol 供 HealthRouter 派生 endpointId）
        ModelInstance modelInstance = instanceSelector.select(model.getId(), applicationId, userId, role, strategy, protocol);

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
