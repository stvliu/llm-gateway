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
package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.user.UserService;
import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.domain.iam.exception.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * UserController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 测试")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    @Nested
    @DisplayName("create 方法测试")
    class CreateTests {

        @Test
        @DisplayName("创建用户成功")
        void create_validRequest_returnsCreated() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("testuser");

            UserResponse response = createTestResponse();
            when(userService.create(any())).thenReturn(response);

            // when
            UserResponse result = controller.create(request);

            // then
            assertThat(result.getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("获取用户详情成功")
        void getById_existingId_returnsUser() {
            // given
            UserResponse response = createTestResponse();
            when(userService.getById(1L)).thenReturn(response);

            // when
            UserResponse result = controller.getById(1L);

            // then
            assertThat(result.getId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("query 方法测试")
    class QueryTests {

        @Test
        @DisplayName("查询用户列表")
        void query_validRequest_returnsPage() {
            // given
            UserResponse response = createTestResponse();
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                List.of(response), 1, 10, 1L
            );
            when(userService.query(any(UserQueryRequest.class))).thenReturn(pageResponse);

            // when
            PageResponse<UserResponse> result = controller.query(new UserQueryRequest());

            // then
            assertThat(result.getItems()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 方法测试")
    class UpdateTests {

        @Test
        @DisplayName("更新用户成功")
        void update_validRequest_returnsUpdated() {
            // given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setEmail("new@example.com");

            UserResponse response = createTestResponse();
            when(userService.update(eq(1L), any())).thenReturn(response);

            // when
            UserResponse result = controller.update(1L, request);

            // then
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("delete 方法测试")
    class DeleteTests {

        @Test
        @DisplayName("删除用户成功")
        void delete_existingId_returnsSuccess() {
            // given
            doNothing().when(userService).delete(1L);

            // when
            controller.delete(1L);

            // then - void 方法，无返回值验证
            // 方法执行成功即测试通过
        }
    }

    @Nested
    @DisplayName("updateState 方法测试")
    class UpdateStateTests {

        @Test
        @DisplayName("更新用户状态成功")
        void updateState_validRequest_returnsUpdated() {
            // given
            UserStateUpdateRequest request = new UserStateUpdateRequest();
            request.setState(UserState.INACTIVE);

            UserResponse response = createTestResponse();
            response.setState(UserState.INACTIVE);
            when(userService.updateState(eq(1L), any())).thenReturn(response);

            // when
            UserResponse result = controller.updateState(1L, request);

            // then
            assertThat(result.getState()).isEqualTo(UserState.INACTIVE);
        }
    }

    @Nested
    @DisplayName("assignRoles 方法测试")
    class AssignRolesTests {

        @Test
        @DisplayName("分配用户角色成功")
        void assignRoles_validRequest_returnsUpdated() {
            // given
            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of("ADMIN"));

            UserResponse response = createTestResponse();
            response.setRole("ADMIN");
            when(userService.assignRoles(eq(1L), any())).thenReturn(response);

            // when
            UserResponse result = controller.assignRoles(1L, request);

            // then
            assertThat(result.getRole()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("resetPassword 方法测试")
    class ResetPasswordTests {

        @Test
        @DisplayName("重置密码成功 — 返回 16 位明文")
        void resetPassword_success_returnsPlaintext() {
            // given — UserService 生成 16 位随机密码（排除易混字符）一次性返回
            when(userService.resetPassword(1L))
                    .thenReturn(new ResetPasswordResponse("AbcdefghijKmnp23"));

            // when
            ResetPasswordResponse result = controller.resetPassword(1L);

            // then
            assertThat(result.newPassword()).isEqualTo("AbcdefghijKmnp23");
            assertThat(result.newPassword()).hasSize(16);
        }

        @Test
        @DisplayName("重置内建用户密码 — 抛 ForbiddenException（映射 403）")
        void resetPassword_builtinUser_throwsForbidden() {
            // given — 内建用户禁止重置密码，UserService 抛 ForbiddenException
            when(userService.resetPassword(2L))
                    .thenThrow(new ForbiddenException("不允许重置系统内建用户的密码"));

            // when & then — Controller 透传异常，由 IamExceptionHandler 映射 403
            assertThatThrownBy(() -> controller.resetPassword(2L))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("内建用户");
        }
    }

    // Helper methods
    private UserResponse createTestResponse() {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setUsername("testuser");
        response.setEmail("test@example.com");
        response.setState(UserState.ACTIVE);
        response.setEmailVerified(true);
        response.setRole("USER");
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());
        return response;
    }
}
