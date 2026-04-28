package com.codingas.gateway.infrastructure.router;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 路由分组 DO
 *
 * <p>JPA 实体，对应数据库 route_groups 表。</p>
 */
@Entity
@Table(name = "route_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteGroupDo extends BaseDo {

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
