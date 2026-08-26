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

import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link StpRoleManager} 测试
 *
 * <p>验证 Sa-Token 角色/权限数据源真实解析 users.role（不 mock StpUtil，
 * 确保运行时 {@code hasRole} 有数据来源）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StpRoleManager 测试")
class StpRoleManagerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StpRoleManager service;

    private User userWithRole(String role) {
        User user = new User();
        user.setId(1L);
        user.setRole(role);
        return user;
    }

    @Nested
    @DisplayName("getRoleList 测试")
    class GetRoleListTests {

        @Test
        @DisplayName("ADMIN 用户返回 ADMIN 角色")
        void admin_returnsAdminRole() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole("ADMIN")));

            assertThat(service.getRoleList(1L, "login")).containsExactly("ADMIN");
        }

        @Test
        @DisplayName("USER 用户返回 USER 角色")
        void user_returnsUserRole() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole("USER")));

            assertThat(service.getRoleList(1L, "login")).containsExactly("USER");
        }

        @Test
        @DisplayName("用户不存在返回空集合")
        void userNotFound_returnsEmpty() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThat(service.getRoleList(999L, "login")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPermissionList 测试")
    class GetPermissionListTests {

        @Test
        @DisplayName("USER 用户返回受限权限码")
        void user_returnsRestrictedPermissions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole("USER")));

            List<String> permissions = service.getPermissionList(1L, "login");

            assertThat(permissions).contains("quickstart:access", "key:write");
            assertThat(permissions).doesNotContain("user:read", "channel:write");
        }

        @Test
        @DisplayName("ADMIN 用户返回全部权限码")
        void admin_returnsAllPermissions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(userWithRole("ADMIN")));

            List<String> permissions = service.getPermissionList(1L, "login");

            assertThat(permissions).contains("user:write", "dashboard:admin");
        }

        @Test
        @DisplayName("用户不存在返回空集合")
        void userNotFound_returnsEmpty() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThat(service.getPermissionList(999L, "login")).isEmpty();
        }
    }
}
