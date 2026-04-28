package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 权限实体
 *
 * <p>细粒度权限码，定义到具体的操作级别。</p>
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

    @Column(name = "permission_code", nullable = false, unique = true, length = 128)
    private String permissionCode;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 32)
    private String category;
}
