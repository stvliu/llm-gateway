/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.iamdata.dataobject;

import com.codingas.gateway.iam.user.UserState;
import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 用户 DO
 *
 * <p>JPA 实体，对应数据库 users 表。</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDo extends BaseDo {

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "email", length = 128)
    private String email;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private UserState state = UserState.ACTIVE;

    /**
     * 用户角色：ADMIN（管理员）/ USER（普通用户）
     */
    @Column(name = "role", length = 32)
    private String role = "USER";

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "oauth_providers", columnDefinition = "json")
    private Map<String, String> oauthProviders;

    @Column(name = "pii_salt", length = 64)
    private String piiSalt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * 是否为系统内建用户（不可删除、不可降级、不可禁用）
     */
    @Column(name = "builtin", nullable = false)
    private Boolean builtin = false;
}
