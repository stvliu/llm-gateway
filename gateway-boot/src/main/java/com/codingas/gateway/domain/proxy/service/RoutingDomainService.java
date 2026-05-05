package com.codingas.gateway.domain.proxy.service;

import com.codingas.gateway.domain.proxy.entity.RouteGroup;
import com.codingas.gateway.domain.proxy.gateway.RouteGroupGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 路由领域服务
 *
 * <p>负责路由分组的核心业务规则。</p>
 * <p>只依赖 proxy 域内的 Gateway，不跨域访问。</p>
 *
 * <p>注意：渠道选择逻辑涉及 model 域和 proxy 域的协调，由 Application 层负责。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingDomainService {

    private final RouteGroupGateway routeGroupGateway;

    /**
     * 根据分组代码查找路由分组
     *
     * @param groupCode 分组代码
     * @return 路由分组
     */
    public Optional<RouteGroup> findByGroupCode(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return Optional.empty();
        }
        RouteGroup group = routeGroupGateway.findByGroupCode(groupCode);
        return Optional.ofNullable(group);
    }

    /**
     * 查找所有活跃路由分组
     *
     * @return 活跃路由分组列表
     */
    public List<RouteGroup> findAllActive() {
        return routeGroupGateway.findAllActive();
    }

    /**
     * 验证路由分组是否可用
     *
     * @param groupCode 分组代码
     * @return true 如果分组存在且启用
     */
    public boolean isRouteGroupAvailable(String groupCode) {
        return findByGroupCode(groupCode)
                .map(RouteGroup::isEnabled)
                .orElse(false);
    }

    /**
     * 获取路由策略
     *
     * <p>如果分组不存在或未启用，返回默认策略。</p>
     *
     * @param groupCode 分组代码
     * @return 路由策略
     */
    public RouteGroup.RoutingStrategy getRoutingStrategy(String groupCode) {
        return findByGroupCode(groupCode)
                .filter(RouteGroup::isEnabled)
                .map(RouteGroup::getStrategy)
                .orElse(RouteGroup.RoutingStrategy.WEIGHTED);
    }

    /**
     * 保存路由分组
     *
     * @param routeGroup 路由分组
     * @return 保存后的实体
     */
    public RouteGroup save(RouteGroup routeGroup) {
        return routeGroupGateway.save(routeGroup);
    }
}
