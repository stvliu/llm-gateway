package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 角色实体
 *
 * <p>权限集合，用于简化权限管理和批量授权。</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

    @Column(name = "role_code", nullable = false, unique = true, length = 64)
    private String roleCode;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private RoleType roleType = RoleType.CUSTOM;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum RoleType {
        /** 预定义角色 */
        SYSTEM,
        /** 自定义角色 */
        CUSTOM
    }
}
