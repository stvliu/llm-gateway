package com.codingas.gateway.infrastructure.model.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 路由分组与提供商关联 DO
 *
 * <p>JPA 实体，对应数据库 route_group_providers 表。</p>
 */
@Entity
@Table(name = "route_group_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteGroupProviderDo extends BaseDo {

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
