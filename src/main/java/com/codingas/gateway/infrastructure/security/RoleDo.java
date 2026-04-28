package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 角色 DO
 *
 * <p>JPA 实体，对应数据库 roles 表。</p>
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleDo extends BaseDo {

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
        SYSTEM,
        CUSTOM
    }
}
