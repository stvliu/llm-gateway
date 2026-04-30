package com.codingas.gateway.domain.quota.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 限流配置实体
 *
 * <p>定义令牌桶算法的限流参数。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class RateLimitConfig extends BaseEntity {

    private String configCode;

    private String name;

    private Integer requestsPerMinute;

    private Integer bucketSize;

    private Integer refillRate;

    private Boolean enabled;

    /**
     * 检查配置是否启用
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}
