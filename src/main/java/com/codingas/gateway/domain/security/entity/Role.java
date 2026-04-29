package com.codingas.gateway.domain.security.entity;
import com.codingas.gateway.domain.DomainEntity;
import com.codingas.gateway.domain.BaseEntity;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 角色实体
 *
 * <p>权限集合，用于简化权限管理和批量授权。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@DomainEntity
@Slf4j
public class Role extends BaseEntity {

    private String roleCode;

    private String name;

    private String description;

    private RoleType roleType = RoleType.CUSTOM;

    private Boolean isActive = true;

    private Instant deletedAt;

    public enum RoleType {
        /** 预定义角色 */
        SYSTEM,
        /** 自定义角色 */
        CUSTOM
    }

    /**
     * 检查角色是否激活
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }
}
