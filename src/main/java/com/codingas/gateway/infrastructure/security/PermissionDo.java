package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 权限 DO
 *
 * <p>JPA 实体，对应数据库 permissions 表。</p>
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDo extends BaseDo {

    @Column(name = "permission_code", nullable = false, unique = true, length = 128)
    private String permissionCode;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 32)
    private String category;
}
