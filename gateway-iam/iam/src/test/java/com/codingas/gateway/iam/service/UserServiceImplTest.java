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
package com.codingas.gateway.iam.service;

import com.codingas.gateway.iam.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.iam.user.UserState;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.exception.ForbiddenException;
import com.codingas.gateway.iam.user.UserGateway;
import com.codingas.gateway.iam.encryption.PasswordEncoder;
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
    private UserGateway userGateway;

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
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("testuser");
            request.setEmail("test@example.com");
            request.setPassword("password123");

            when(userGateway.existsByEmail("test@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userGateway.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // when
            UserResponse result = service.create(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("邮箱重复抛出异常")
        void create_duplicateEmail_throwsException() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setEmail("test@example.com");

            when(userGateway.existsByEmail("test@example.com")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.create(request))
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
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));

            // when
            UserResponse result = service.getById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("用户不存在抛出异常")
        void getById_notFound_throwsException() {
            // given
            when(userGateway.findById(999L)).thenReturn(Optional.empty());

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
            when(userGateway.findAll()).thenReturn(List.of(user));

            UserQueryRequest request = new UserQueryRequest();
            request.setPage(1);
            request.setLimit(10);

            // when
            PageResponse<UserResponse> result = service.query(request);

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
            when(userGateway.findAll()).thenReturn(List.of(user1, user2));

            UserQueryRequest request = new UserQueryRequest();
            request.setKeyword("test");
            request.setPage(1);
            request.setLimit(10);

            // when
            PageResponse<UserResponse> result = service.query(request);

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
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(userGateway.save(any())).thenReturn(user);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername("newname");

            // when
            UserResponse result = service.update(1L, request);

            // then
            assertThat(result).isNotNull();
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
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(userGateway.save(any())).thenReturn(user);

            // when
            service.delete(1L);

            // then
            verify(userGateway).save(any());
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
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(userGateway.save(any())).thenReturn(user);

            UserStateUpdateRequest request = new UserStateUpdateRequest();
            request.setState(UserState.INACTIVE);

            // when
            UserResponse result = service.updateState(1L, request);

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
            when(userGateway.findById(1L)).thenReturn(Optional.of(user));
            when(userGateway.save(any())).thenReturn(user);

            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of("ADMIN"));

            // when
            UserResponse result = service.assignRoles(1L, request);

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
            when(userGateway.findById(1L)).thenReturn(java.util.Optional.of(user));
            when(passwordEncoder.encode(any())).thenReturn("hashed");

            ResetPasswordResponse response = service.resetPassword(1L);

            assertThat(response.newPassword()).hasSize(16);
            // 排除易混字符 O/0/I/1/l
            assertThat(response.newPassword()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789]{16}");
            verify(userGateway).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
        }

        @Test
        @DisplayName("内建用户拒绝重置")
        void resetPassword_builtin_throws() {
            User user = new User();
            user.setId(2L);
            user.setBuiltin(true);
            when(userGateway.findById(2L)).thenReturn(java.util.Optional.of(user));

            assertThatThrownBy(() -> service.resetPassword(2L))
                    .isInstanceOf(ForbiddenException.class);
            verify(userGateway, never()).save(any());
        }

        @Test
        @DisplayName("用户不存在 — 抛 ResourceNotFoundException")
        void resetPassword_notFound_throws() {
            when(userGateway.findById(99L)).thenReturn(java.util.Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
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
}
