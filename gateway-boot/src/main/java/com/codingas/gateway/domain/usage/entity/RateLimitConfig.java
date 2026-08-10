/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.domain.usage.entity;
import com.codingas.gateway.common.entity.DomainEntity;
import com.codingas.gateway.common.entity.BaseEntity;

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
