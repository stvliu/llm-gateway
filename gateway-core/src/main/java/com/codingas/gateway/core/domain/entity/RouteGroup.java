package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 路由分组
 *
 * <p>用于负载均衡和故障转移的路由策略配置。</p>
 * <p>属于系统全局，所有用户共享。</p>
 */
@Entity
@Table(name = "route_groups")
@Getter
@Setter
public class RouteGroup extends BaseEntity {

    /**
     * 分组编码 (业务标识)
     */
    @Column(name = "group_code", nullable = false, unique = true, length = 64)
    private String groupCode;

    /**
     * 分组名称
     */
    @Column(name = "group_name", nullable = false, length = 128)
    private String groupName;

    /**
     * 路由策略
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 32)
    private RoutingStrategy strategy = RoutingStrategy.PRIORITY;

    /**
     * 是否启用故障转移
     */
    @Column(name = "failover_enabled", nullable = false)
    private Boolean failoverEnabled = true;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retry")
    private Integer maxRetry = 2;

    /**
     * 健康检查间隔（秒）
     */
    @Column(name = "health_check_interval")
    private Integer healthCheckInterval = 30;

    /**
     * 描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 路由策略枚举
     */
    public enum RoutingStrategy {
        /** 按权重轮询 */
        ROUND_ROBIN,
        /** 选择延迟最低的 Provider */
        LEAST_LATENCY,
        /** 优先使用高优先级，失败后 failover */
        PRIORITY
    }
}
