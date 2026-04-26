package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 限流配置实体
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