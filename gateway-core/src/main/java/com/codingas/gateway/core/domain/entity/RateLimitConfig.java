package com.codingas.gateway.core.domain.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 限流配置实体
 *
 * <p>定义不同级别的请求限流策略，支持令牌桶算法。</p>
 *
 * <p>表名: rate_limit_configs</p>
 *
 * @see BaseEntity
 */
@Entity
@Table(
    name = "rate_limit_configs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_rate_limit_configs_config_code", columnNames = "config_code")
    }
)
@Getter
@Setter
public class RateLimitConfig extends BaseEntity {

    /**
     * 配置编码 (业务标识, 如 "default", "premium")
     */
    @Column(name = "config_code", nullable = false, unique = true, length = 64)
    private String configCode;

    /**
     * 显示名称
     */
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    /**
     * 每分钟最大请求数
     */
    @Column(name = "requests_per_minute", nullable = false)
    private Integer requestsPerMinute;

    /**
     * 每小时最大请求数 (可选)
     */
    @Column(name = "requests_per_hour")
    private Integer requestsPerHour;

    /**
     * 每天最大请求数 (可选)
     */
    @Column(name = "requests_per_day")
    private Integer requestsPerDay;

    /**
     * 令牌桶大小 (突发容量)
     */
    @Column(name = "bucket_size", nullable = false)
    private Integer bucketSize;

    /**
     * 每秒补充的令牌数
     */
    @Column(name = "refill_rate", nullable = false)
    private Integer refillRate;

    /**
     * 配置是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}
