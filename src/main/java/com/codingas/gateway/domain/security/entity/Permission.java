package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限实体
 *
 * <p>细粒度权限码，定义到具体的操作级别。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Permission extends BaseEntity {

    private String permissionCode;

    private String name;

    private String description;

    private String category;
}
