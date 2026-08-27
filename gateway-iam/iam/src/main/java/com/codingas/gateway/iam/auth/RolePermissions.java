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
package com.codingas.gateway.iam.auth;

import java.util.List;

/**
 * 角色 → 权限码 推导（后端为权威，供前端 UI 显隐使用）
 *
 * <p>授权本身基于 USER/ADMIN 两角色（由 PermissionInterceptor 白名单强制）；
 * 本类仅负责把 role 推导为前端 UI 所需的权限码列表，随登录响应返回，
 * 前端直接消费、不自行维护映射，从而避免前后端权限定义漂移。</p>
 *
 * <p>USER 权限码与 {@code PermissionInterceptor} 的 USER 白名单语义一一对应：
 * 仪表盘、模型只读、体验中心、自己的 API Key、应用只读。</p>
 */
public final class RolePermissions {

    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    /** 普通用户 / 开发者角色 */
    public static final String ROLE_USER = "USER";

    /** USER 角色权限码（与 PermissionInterceptor USER 白名单对齐） */
    private static final List<String> USER_PERMISSIONS = List.of(
            "dashboard",
            "model:read",
            "quickstart:access",
            "key:read",
            "key:write",
            "application:read"
    );

    /** ADMIN 角色权限码（全部管理能力） */
    private static final List<String> ADMIN_PERMISSIONS = List.of(
            "dashboard", "dashboard:admin",
            "model:read", "model:write",
            "provider:read", "provider:write",
            "catalog:read", "catalog:write",
            "user:read", "user:write",
            "settings:read", "settings:write",
            "key:read", "key:write",
            "channel:read", "channel:write",
            "application:read", "application:write",
            "resilience:read", "resilience:write",
            "quickstart:access",
            "audit:read",
            "token-limit:manage"
    );

    /**
     * 根据角色推导权限码列表（供登录响应返回）
     *
     * @param role 用户角色（users.role 字段）
     * @return 该角色的权限码；未知角色返回空列表
     */
    public static List<String> of(String role) {
        if (ROLE_ADMIN.equals(role)) {
            return ADMIN_PERMISSIONS;
        }
        if (ROLE_USER.equals(role)) {
            return USER_PERMISSIONS;
        }
        return List.of();
    }

    private RolePermissions() {
        // 工具类禁止实例化
    }
}
