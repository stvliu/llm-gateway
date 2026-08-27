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
package com.codingas.gateway.iam.user;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.iam.auth.AuthenticationFailedException;
import com.codingas.gateway.iam.encryption.PasswordEncoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 测试")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl service;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建用户成功")
        void create_validRequest_returnsCreated() {
            // given
            User user = createUser("testuser", "test@example.com", null, null);

            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // when
            User result = service.create(user, "password123");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("邮箱重复抛出异常")
        void create_duplicateEmail_throwsException() {
            // given
            User user = createUser("testuser", "test@example.com", null, null);

            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.create(user, "password123"))
                .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取用户成功")
        void getById_existingId_returnsUser() {
            // given
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            // when
            User result = service.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void getById_notFound_throwsException() {
            // given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询用户列表")
        void query_validRequest_returnsPage() {
            // given
            User user = createTestUser();
            when(userRepository.findAll()).thenReturn(List.of(user));

            UserQuery query = new UserQuery();
            query.setPage(1);
            query.setLimit(10);

            // when
            PageResponse<User> result = service.query(query);

            // then
            assertThat(result.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("按关键字过滤")
        void query_withKeyword_filtersResults() {
            // given
            User user1 = createTestUser();
            User user2 = createTestUser();
            user2.setId(2L);
            user2.setUsername("other");
            user2.setEmail("other@example.com");
            when(userRepository.findAll()).thenReturn(List.of(user1, user2));

            UserQuery query = new UserQuery();
            query.setKeyword("test");
            query.setPage(1);
            query.setLimit(10);

            // when
            PageResponse<User> result = service.query(query);

            // then
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新用户成功")
        void update_validRequest_returnsUpdated() {
            // given
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            User patch = createUser("newname", null, null, null);

            // when
            User result = service.update(1L, patch);

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("更新手机号与头像")
        void update_phoneAndAvatar_updatesFields() {
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User patch = createUser(null, null, "13900000000", "https://example.com/a.png");

            User result = service.update(1L, patch);

            assertThat(result.getPhone()).isEqualTo("13900000000");
            assertThat(result.getAvatarUrl()).isEqualTo("https://example.com/a.png");
            verify(userRepository).save(argThat(u -> "13900000000".equals(u.getPhone())));
        }

        @Test
        @DisplayName("更新不存在用户抛出异常")
        void update_notFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, createUser(null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除用户成功")
        void delete_existingId_success() {
            // given
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            // when
            service.delete(1L);

            // then
            verify(userRepository).save(any());
        }
    }

    @Nested
    @DisplayName("updateStatus 方法测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新用户状态成功")
        void updateStatus_validRequest_returnsUpdated() {
            // given
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            // when
            User result = service.updateState(1L, UserState.INACTIVE);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("assignRoles 方法测试")
    class AssignRolesTests {

        @Test
        @DisplayName("分配角色成功")
        void assignRoles_validRequest_returnsUpdated() {
            // given
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);

            // when
            User result = service.assignRoles(1L, List.of("ADMIN"));

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("resetPassword 方法测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置成功 — 返回 16 位明文且更新哈希")
        void resetPassword_success_returns16CharPlain() {
            User user = new User();
            user.setId(1L);
            user.setBuiltin(false);
            when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            ResetPasswordResult result = service.resetPassword(1L);

            assertThat(result.newPassword()).hasSize(16);
            // 排除易混字符 O/0/I/1/l
            assertThat(result.newPassword()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789]{16}");
            verify(userRepository).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
        }

        @Test
        @DisplayName("内建用户拒绝重置")
        void resetPassword_builtin_throws() {
            User user = new User();
            user.setId(2L);
            user.setBuiltin(true);
            when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(user));

            assertThatThrownBy(() -> service.resetPassword(2L))
                    .isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在 — 抛 ResourceNotFoundException")
        void resetPassword_notFound_throws() {
            when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("内建用户保护分支测试")
    class BuiltinGuardTests {

        private User builtinUser() {
            User user = createTestUser();
            user.setBuiltin(true);
            return user;
        }

        @Test
        @DisplayName("删除内建用户 — 抛 ForbiddenException")
        void delete_builtin_throws() {
            User user = builtinUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.delete(1L))
                    .isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("禁用内建用户 — 抛 ForbiddenException")
        void updateState_builtinInactive_throws() {
            User user = builtinUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.updateState(1L, UserState.INACTIVE))
                    .isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("内建用户保持 ACTIVE — 允许")
        void updateState_builtinActive_allowed() {
            User user = builtinUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = service.updateState(1L, UserState.ACTIVE);
            assertThat(result).isNotNull();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("变更内建用户角色 — 抛 ForbiddenException")
        void assignRoles_builtin_throws() {
            User user = builtinUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.assignRoles(1L, List.of("ADMIN")))
                    .isInstanceOf(ForbiddenException.class);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("空角色列表 — 不修改角色直接保存")
        void assignRoles_emptyRoleCodes_keepsRole() {
            User user = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = service.assignRoles(1L, List.of());

            assertThat(result.getRole()).isEqualTo("USER");
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("login 方法测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功 — 返回 token、注入权限并更新最后登录时间")
        void login_success_returnsToken() {
            User user = createTestUser();
            user.setPasswordHash("hash");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("pass", "hash")).thenReturn(true);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
                stp.when(() -> StpUtil.getTokenValue()).thenReturn("token-123");

                LoginResult result = service.login("testuser", "pass", false);

                assertThat(result.token()).isEqualTo("token-123");
                assertThat(result.user().getUsername()).isEqualTo("testuser");
                assertThat(result.user().getRole()).isEqualTo("USER");
                // 登录结果携带角色推导的权限码（前端 UI 直接消费）
                assertThat(result.permissions()).contains("quickstart:access");
                assertThat(result.permissions()).doesNotContain("user:read");
                stp.verify(() -> StpUtil.login(1L));
                assertThat(user.getLastLoginAt()).isNotNull();
            }
        }

        @Test
        @DisplayName("用户不存在 — 抛 AuthenticationFailedException")
        void login_userNotFound_throws() {
            when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login("nobody", "x", false))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("用户名或密码错误");
        }

        @Test
        @DisplayName("用户被禁用 — 抛 AuthenticationFailedException")
        void login_inactiveUser_throws() {
            User user = createTestUser();
            user.setState(UserState.INACTIVE);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.login("testuser", "pass", false))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessageContaining("用户已被禁用");
        }

        @Test
        @DisplayName("密码错误 — 抛 AuthenticationFailedException")
        void login_wrongPassword_throws() {
            User user = createTestUser();
            user.setPasswordHash("hash");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

            assertThatThrownBy(() -> service.login("testuser", "wrong", false))
                    .isInstanceOf(AuthenticationFailedException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changePassword 方法测试")
    class ChangePasswordTests {

        @Test
        @DisplayName("修改密码成功")
        void changePassword_success_updatesHash() {
            User user = createTestUser();
            user.setPasswordHash("old-hash");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old", "old-hash")).thenReturn(true);
            when(passwordEncoder.encode("new")).thenReturn("new-hash");

            service.changePassword(1L, "old", "new");

            verify(userRepository).save(argThat(u -> "new-hash".equals(u.getPasswordHash())));
        }

        @Test
        @DisplayName("当前密码错误 — 抛 GatewayRequestException")
        void changePassword_wrongCurrent_throws() {
            User user = createTestUser();
            user.setPasswordHash("old-hash");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

            assertThatThrownBy(() -> service.changePassword(1L, "wrong", "new"))
                    .isInstanceOf(GatewayRequestException.class)
                    .satisfies(ex -> assertThat(((GatewayRequestException) ex).getCode())
                            .isEqualTo("INVALID_PASSWORD"));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在 — 抛 ResourceNotFoundException")
        void changePassword_notFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changePassword(99L, "a", "b"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("logout 方法测试")
    class LogoutTests {

        @Test
        @DisplayName("登出 — 调用 StpUtil.logout")
        void logout_callsSaTokenLogout() {
            try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
                service.logout();
                stp.verify(StpUtil::logout);
            }
        }
    }

    // Helper methods
    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setState(UserState.ACTIVE);
        user.setRole("USER");
        return user;
    }

    /** 构造用户实体（create/update 共用：null 字段表示不更新） */
    private User createUser(String username, String email, String phone, String avatarUrl) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAvatarUrl(avatarUrl);
        return user;
    }
}
