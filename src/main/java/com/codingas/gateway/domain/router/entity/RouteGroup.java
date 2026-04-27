package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 路由分组实体
 *
 * <p>定义模型的路由分组和路由策略。</p>
 */
@Entity
@Table(name = "route_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteGroup extends BaseEntity {

    @Column(name = "group_code", nullable = false, unique = true, length = 64)
    private String groupCode;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false)
    private RoutingStrategy strategy;

    @Column(name = "enabled")
    private Boolean enabled;

    public enum RoutingStrategy {
        RANDOM, WEIGHTED, FAILOVER, COST_OPTIMIZED, LATENCY_OPTIMIZED
    }
}
