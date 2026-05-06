package com.codingas.gateway.domain.proxy.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 路由分组实体
 *
 * <p>定义模型的路由分组和路由策略。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class RouteGroup extends BaseEntity {

    private String groupName;

    private RoutingStrategy strategy;

    private Boolean enabled;

    public enum RoutingStrategy {
        RANDOM, WEIGHTED, FAILOVER, COST_OPTIMIZED, LATENCY_OPTIMIZED
    }

    /**
     * 检查路由组是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
