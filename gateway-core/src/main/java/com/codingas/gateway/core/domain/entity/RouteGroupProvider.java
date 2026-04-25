package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 路由分组与Provider关联
 *
 * <p>定义某个 RouteGroup 下可用的 Provider 列表及其权重/优先级。</p>
 */
@Entity
@Table(name = "route_group_providers", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"route_group_id", "provider_id"})
})
@Getter
@Setter
public class RouteGroupProvider extends BaseEntity {

    /**
     * 路由分组 ID
     */
    @Column(name = "route_group_id", nullable = false)
    private Long routeGroupId;

    /**
     * Provider ID
     */
    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    /**
     * 权重（用于负载均衡）
     */
    @Column(name = "weight")
    private Integer weight = 100;

    /**
     * 优先级（用于故障转移，数值越大越优先）
     */
    @Column(name = "priority")
    private Integer priority = 100;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RouteGroupProviderStatus status = RouteGroupProviderStatus.ENABLED;

    /**
     * 健康状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 32)
    private HealthStatus healthStatus = HealthStatus.HEALTHY;

    /**
     * 连续失败次数
     */
    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    /**
     * 最后健康检查时间
     */
    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    /**
     * 关联状态枚举
     */
    public enum RouteGroupProviderStatus {
        /** 可用 */
        ENABLED,
        /** 管理员禁用 */
        DISABLED,
        /** 健康检查失败 */
        UNHEALTHY
    }

    /**
     * 健康状态枚举
     */
    public enum HealthStatus {
        /** 正常 */
        HEALTHY,
        /** 部分失败，响应慢 */
        DEGRADED,
        /** 连续失败，不可用 */
        UNHEALTHY
    }
}
