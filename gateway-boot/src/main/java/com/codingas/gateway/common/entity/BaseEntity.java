/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.common.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 领域实体基类
 *
 * <p>提供公共字段，无 JPA 依赖。领域实体应继承此类。</p>
 *
 * <p>注意：version 字段是 JPA/数据库层的实现细节，不应暴露给领域层。</p>
 * <p>乐观锁由 Gateway 实现层处理。</p>
 *
 * <p>审计字段：</p>
 * <ul>
 *   <li>createdBy - 创建人ID</li>
 *   <li>createdAt - 创建时间</li>
 *   <li>updatedBy - 更新人ID</li>
 *   <li>updatedAt - 更新时间</li>
 * </ul>
 */
@Data
@Slf4j
public abstract class BaseEntity {

    protected Long id;

    /**
     * 创建人ID
     */
    protected Long createdBy;

    /**
     * 创建时间
     */
    protected Instant createdAt;

    /**
     * 更新人ID
     */
    protected Long updatedBy;

    /**
     * 更新时间
     */
    protected Instant updatedAt;
}
