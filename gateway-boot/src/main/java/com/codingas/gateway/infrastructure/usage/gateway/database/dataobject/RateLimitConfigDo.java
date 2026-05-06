package com.codingas.gateway.infrastructure.usage.gateway.database.dataobject;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 限流配置 DO
 *
 * <p>JPA 实体，对应数据库 rate_limit_configs 表。</p>
 */
@Entity
@Table(name = "rate_limit_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfigDo extends BaseDo {

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
