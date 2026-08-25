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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RolePermissions} 角色 → 权限码推导测试
 */
@DisplayName("RolePermissions 测试")
class RolePermissionsTest {

    @Test
    @DisplayName("ADMIN 角色推导出全部管理权限码")
    void admin_holdsAllPermissions() {
        List<String> permissions = RolePermissions.of("ADMIN");

        assertThat(permissions).contains("user:write", "channel:write", "dashboard:admin", "quickstart:access");
        assertThat(permissions).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("USER 角色推导出受限权限码（与 PermissionInterceptor 白名单对齐）")
    void user_holdsRestrictedPermissions() {
        List<String> permissions = RolePermissions.of("USER");

        // 普通用户可用：仪表盘 / 模型读 / 体验中心 / Key 读写 / 应用读
        assertThat(permissions).containsExactlyInAnyOrder(
                "dashboard", "model:read", "quickstart:access",
                "key:read", "key:write", "application:read"
        );
        // 不含管理向权限
        assertThat(permissions).doesNotContain("user:read", "channel:write", "dashboard:admin");
    }

    @Test
    @DisplayName("未知角色返回空权限列表")
    void unknownRole_returnsEmpty() {
        assertThat(RolePermissions.of("GUEST")).isEmpty();
        assertThat(RolePermissions.of(null)).isEmpty();
    }
}
