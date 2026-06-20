package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ModelInstanceGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型实例选择器 — 委托给 RouterChain 执行权限过滤 + 优先级分组 + 负载均衡
 */
@Component
@RequiredArgsConstructor
public class InstanceSelector {

    private final ModelInstanceGateway modelInstanceGateway;
    private final RouterChain routerChain;

    /**
     * 根据 modelId 和用户身份选择模型实例
     *
     * @param modelId       模型 ID
     * @param applicationId 应用 ID（权限锚点；透传至 RoutingRequest 供 PermissionRouter 判定可见渠道）
     * @param userId        用户 ID
     * @param role          用户角色
     * @param strategy      路由策略
     * @param protocol      入站协议（透传至 RoutingRequest 供 HealthRouter 按 channelId 派生 endpointId）
     * @return 选中的 ModelInstance
     * @throws ResourceNotFoundException 无可用实例
     */
    public ModelInstance select(Long modelId, Long applicationId, Long userId, String role,
                                RoutingStrategy strategy, Protocol protocol) {
        // 获取所有活跃实例（按 priority 升序）
        List<ModelInstance> allInstances = modelInstanceGateway.findActiveByModelIdOrderByPriority(modelId);
        if (allInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 委托 RouterChain 执行过滤链（applicationId 作为权限锚点、protocol 供 HealthRouter 派生 endpointId）
        RoutingRequest request = new RoutingRequest(modelId, applicationId, userId, role, strategy, protocol);
        List<ModelInstance> result = routerChain.filter(allInstances, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        return result.getFirst();
    }
}