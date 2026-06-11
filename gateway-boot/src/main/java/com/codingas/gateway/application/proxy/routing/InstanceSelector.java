package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
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
     * @param modelId  模型 ID
     * @param userId   用户 ID
     * @param role     用户角色
     * @param strategy 路由策略
     * @return 选中的 ModelInstance
     * @throws ResourceNotFoundException 无可用实例
     */
    public ModelInstance select(Long modelId, Long userId, String role, RoutingStrategy strategy) {
        // 获取所有活跃实例（按 priority 升序）
        List<ModelInstance> allInstances = modelInstanceGateway.findActiveByModelIdOrderByPriority(modelId);
        if (allInstances.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        // 委托 RouterChain 执行过滤链
        RoutingRequest request = new RoutingRequest(modelId, userId, role, strategy);
        List<ModelInstance> result = routerChain.filter(allInstances, request);

        if (result.isEmpty()) {
            throw new ResourceNotFoundException("ModelInstance", modelId);
        }

        return result.getFirst();
    }
}