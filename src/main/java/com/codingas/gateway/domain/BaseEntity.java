package com.codingas.gateway.domain;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 领域实体基类
 *
 * <p>提供公共字段，无 JPA 依赖。领域实体应继承此类。</p>
 */
@Data
@Slf4j
public abstract class BaseEntity {

    protected Long id;

    protected Instant createdAt;

    protected Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
