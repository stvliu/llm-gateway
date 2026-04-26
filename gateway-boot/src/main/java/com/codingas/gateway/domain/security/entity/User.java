package com.codingas.gateway.domain.security.entity;

import com.codingas.gateway.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class User extends BaseEntity {

    @Column(name = "user_code", nullable = false, unique = true, length = 64)
    private String userCode;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "email")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "password_hash")
    private String passwordHash;

    public enum UserRole {
        ADMIN, USER, READONLY
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, DELETED
    }
}