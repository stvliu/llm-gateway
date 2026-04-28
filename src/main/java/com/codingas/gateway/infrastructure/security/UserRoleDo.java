package com.codingas.gateway.infrastructure.security;

import com.codingas.gateway.infrastructure.common.BaseDo;
import jakarta.persistence.*;
import lombok.*;

/**
 * 用户角色关联 DO
 *
 * <p>JPA 实体，对应数据库 user_roles 表。</p>
 */
@Entity
@Table(name = "user_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleDo extends BaseDo {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserDo user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleDo role;
}
