package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.application.user.UserApplication;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.enums.UserStatus;
import com.codingas.gateway.adapter.admin.dto.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserController 集成测试 (WebFlux)
 */
@WebFluxTest(UserController.class)
@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 集成测试")
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserApplication userApplication;

    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        testUserResponse = createTestUserResponse(1L, "USR001", "testuser", "test@example.com");
    }

    // ==================== POST /api/v1/users ====================

    @Nested
    @DisplayName("POST /api/v1/users 创建用户")
    class CreateTests {

        @Test
        @DisplayName("创建用户成功返回 200")
        void create_validRequest_returns200() throws Exception {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setRoleCodes(List.of("ADMIN"));

            when(userApplication.create(any(UserCreateRequest.class))).thenReturn(testUserResponse);

            // when & then
            webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.username").isEqualTo("testuser");

            verify(userApplication).create(any(UserCreateRequest.class));
        }

        @Test
        @DisplayName("空用户名返回 400")
        void create_emptyUsername_returns400() throws Exception {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("");
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setRoleCodes(List.of("ADMIN"));

            // when & then
            webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("无效邮箱格式返回 400")
        void create_invalidEmail_returns400() throws Exception {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("invalid-email");
            request.setPassword("password123");
            request.setRoleCodes(List.of("ADMIN"));

            // when & then
            webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("密码过短返回 400")
        void create_shortPassword_returns400() throws Exception {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("short");
            request.setRoleCodes(List.of("ADMIN"));

            // when & then
            webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("空角色列表返回 400")
        void create_emptyRoleCodes_returns400() throws Exception {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");
            request.setRoleCodes(List.of());

            // when & then
            webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }
    }

    // ==================== GET /api/v1/users/{id} ====================

    @Nested
    @DisplayName("GET /api/v1/users/{id} 获取用户")
    class GetByIdTests {

        @Test
        @DisplayName("用户存在返回 200")
        void getById_existingUser_returns200() {
            // given
            when(userApplication.getById(1L)).thenReturn(testUserResponse);

            // when & then
            webTestClient.get()
                .uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.userCode").isEqualTo("USR001")
                .jsonPath("$.data.username").isEqualTo("testuser");

            verify(userApplication).getById(1L);
        }
    }

    // ==================== GET /api/v1/users ====================

    @Nested
    @DisplayName("GET /api/v1/users 查询用户列表")
    class QueryTests {

        @Test
        @DisplayName("查询用户列表返回 200")
        void query_noParams_returns200() {
            // given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                List.of(testUserResponse), 1, 20, 1
            );
            when(userApplication.query(any(UserQueryRequest.class))).thenReturn(pageResponse);

            // when & then
            webTestClient.get()
                .uri("/api/v1/users?page=1&limit=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.items").isArray()
                .jsonPath("$.data.items[0].id").isEqualTo(1)
                .jsonPath("$.data.pagination.total").isEqualTo(1);
        }

        @Test
        @DisplayName("带关键字查询返回 200")
        void query_withKeyword_returns200() {
            // given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                List.of(testUserResponse), 1, 20, 1
            );
            when(userApplication.query(any(UserQueryRequest.class))).thenReturn(pageResponse);

            // when & then
            webTestClient.get()
                .uri("/api/v1/users?keyword=test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.items").isArray();
        }

        @Test
        @DisplayName("按状态查询返回 200")
        void query_withStatus_returns200() {
            // given
            PageResponse<UserResponse> pageResponse = PageResponse.of(
                List.of(testUserResponse), 1, 20, 1
            );
            when(userApplication.query(any(UserQueryRequest.class))).thenReturn(pageResponse);

            // when & then
            webTestClient.get()
                .uri("/api/v1/users?status=ACTIVE")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
        }
    }

    // ==================== PUT /api/v1/users/{id} ====================

    @Nested
    @DisplayName("PUT /api/v1/users/{id} 更新用户")
    class UpdateTests {

        @Test
        @DisplayName("更新用户成功返回 200")
        void update_validRequest_returns200() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername("updateduser");
            request.setEmail("updated@example.com");

            when(userApplication.update(eq(1L), any(UserUpdateRequest.class)))
                .thenReturn(testUserResponse);

            // when & then
            webTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(1);

            verify(userApplication).update(eq(1L), any(UserUpdateRequest.class));
        }

        @Test
        @DisplayName("无效邮箱格式返回 400")
        void update_invalidEmail_returns400() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setEmail("invalid-email");

            // when & then
            webTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }

        @Test
        @DisplayName("用户名过短返回 400")
        void update_shortUsername_returns400() throws Exception {
            // given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername("a");

            // when & then
            webTestClient.put()
                .uri("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }
    }

    // ==================== DELETE /api/v1/users/{id} ====================

    @Nested
    @DisplayName("DELETE /api/v1/users/{id} 删除用户")
    class DeleteTests {

        @Test
        @DisplayName("删除用户成功返回 200")
        void delete_existingUser_returns200() {
            // given
            doNothing().when(userApplication).delete(1L);

            // when & then
            webTestClient.delete()
                .uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);

            verify(userApplication).delete(1L);
        }
    }

    // ==================== PATCH /api/v1/users/{id}/status ====================

    @Nested
    @DisplayName("PATCH /api/v1/users/{id}/status 更新用户状态")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新状态成功返回 200")
        void updateStatus_validRequest_returns200() throws Exception {
            // given
            UserStatusUpdateRequest request = new UserStatusUpdateRequest();
            request.setStatus(UserStatus.DISABLED);

            when(userApplication.updateStatus(eq(1L), any(UserStatusUpdateRequest.class)))
                .thenReturn(testUserResponse);

            // when & then
            webTestClient.patch()
                .uri("/api/v1/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(1);

            verify(userApplication).updateStatus(eq(1L), any(UserStatusUpdateRequest.class));
        }

        @Test
        @DisplayName("空状态返回 400")
        void updateStatus_nullStatus_returns400() throws Exception {
            // given
            UserStatusUpdateRequest request = new UserStatusUpdateRequest();
            // status 未设置，应触发验证失败

            // when & then
            webTestClient.patch()
                .uri("/api/v1/users/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }
    }

    // ==================== PUT /api/v1/users/{id}/roles ====================

    @Nested
    @DisplayName("PUT /api/v1/users/{id}/roles 分配角色")
    class AssignRolesTests {

        @Test
        @DisplayName("分配角色成功返回 200")
        void assignRoles_validRequest_returns200() throws Exception {
            // given
            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of("ADMIN", "USER"));

            when(userApplication.assignRoles(eq(1L), any(UserRoleAssignRequest.class)))
                .thenReturn(testUserResponse);

            // when & then
            webTestClient.put()
                .uri("/api/v1/users/1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.id").isEqualTo(1);

            verify(userApplication).assignRoles(eq(1L), any(UserRoleAssignRequest.class));
        }

        @Test
        @DisplayName("空角色列表返回 400")
        void assignRoles_emptyRoleCodes_returns400() throws Exception {
            // given
            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of());

            // when & then
            webTestClient.put()
                .uri("/api/v1/users/1/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest();
        }
    }

    // ==================== 辅助方法 ====================

    private UserResponse createTestUserResponse(Long id, String userCode, String username, String email) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setUserCode(userCode);
        response.setUsername(username);
        response.setEmail(email);
        response.setPhone("13800138000");
        response.setStatus(UserStatus.ACTIVE);
        response.setEmailVerified(false);
        response.setLastLoginAt(Instant.now());
        response.setCreatedAt(Instant.now());
        response.setUpdatedAt(Instant.now());

        UserResponse.RoleInfo roleInfo = new UserResponse.RoleInfo();
        roleInfo.setRoleCode("ADMIN");
        roleInfo.setName("Administrator");
        response.setRoles(List.of(roleInfo));

        return response;
    }
}
