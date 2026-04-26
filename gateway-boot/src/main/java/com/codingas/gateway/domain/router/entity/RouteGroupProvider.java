package com.codingas.gateway.domain.router.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 路由分组与提供商的关联实体
 *
 * <p>表示路由分组与提供商之间的多对多关系。</p>
 */
@Entity
@Table(name = "route_group_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteGroupProvider extends BaseEntity {

    @Column(name = "route_group_id", nullable = false)
    private Long routeGroupId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "weight")
    private Integer weight;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "enabled")
    private Boolean enabled;
}
