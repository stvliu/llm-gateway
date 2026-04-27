package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 限流配置实体
 *
 * <p>定义令牌桶算法的限流参数。</p>
 * <ul>
 *   <li>requestsPerMinute: 每分钟允许的请求数</li>
 *   <li>bucketSize: 令牌桶容量</li>
 *   <li>refillRate: 令牌补充速率（每分钟补充的令牌数）</li>
 * </ul>
 */
@Entity
@Table(name = "rate_limit_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig extends BaseEntity {

    @Column(name = "config_code", nullable = false, unique = true, length = 64)
    private String configCode;

    @Column(name = "name")
    private String name;

    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Column(name = "bucket_size")
    private Integer bucketSize;

    @Column(name = "refill_rate")
    private Integer refillRate;

    @Column(name = "enabled")
    private Boolean enabled;
}