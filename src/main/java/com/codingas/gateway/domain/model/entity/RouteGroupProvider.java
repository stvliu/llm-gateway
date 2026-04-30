package com.codingas.gateway.domain.model.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 路由分组与提供商的关联实体
 *
 * <p>表示路由分组与提供商之间的多对多关系。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class RouteGroupProvider extends BaseEntity {

    private Long routeGroupId;

    private Long providerId;

    private Integer weight;

    private Integer priority;

    private Boolean enabled;

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
