package com.codingas.gateway.application.user;

import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.infrastructure.config.SecurityConfig.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Mock
    private UserGateway userGateway;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = createTestUser(1L, "testuser", "test@example.com");
    }

    // ==================== create 测试 ====================

    @Nested
    @DisplayName("create 创建用户")
    class CreateTests {

        @Test
        @DisplayName("创建用户成功")
        void create_validRequest_returnsUserResponse() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("plainPassword123");
            request.setPhone("13800138000");
            request.setRole("ADMIN");

            when(userGateway.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword");
            when(userGateway.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // when
            UserResponse response = userService.create(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getUsername()).isEqualTo("newuser");
            assertThat(response.getEmail()).isEqualTo("new@example.com");
            verify(userGateway).existsByEmail("new@example.com");
            verify(userGateway).save(any(User.class));
        }

        @Test
        @DisplayName("创建用户时角色默认为 USER")
        void create_withoutRole_defaultsToUser() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("plainPassword123");

            when(userGateway.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword");
            when(userGateway.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // when
            userService.create(request);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userGateway).save(userCaptor.capture());
            // 默认值在 User 实体中定义为 "USER"
            assertThat(userCaptor.getValue().getRole()).isEqualTo("USER");
        }

        @Test
        @DisplayName("邮箱已存在时抛出 DuplicateResourceException")
        void create_duplicateEmail_throwsException() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("existing@example.com");
            request.setPassword("password123");

            when(userGateway.existsByEmail("existing@example.com")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("email");

            verify(userGateway, never()).save(any(User.class));
        }

        @Test
        @DisplayName("创建用户时密码应被哈希")
        void create_validRequest_passwordIsHashed() {
            // given
            UserCreateRequest request = new UserCreateRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("plainPassword123");

            when(userGateway.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plainPassword123")).thenReturn("encodedPassword");
            when(userGateway.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(2L);
                return user;
            });

            // when
            userService.create(request);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userGateway).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
        }
    }

    // ==================== getById 测试 ====================

    @Nested
    @DisplayName("getById 获取用户")
    class GetByIdTests {

        @Test
        @DisplayName("用户存在时返回用户响应")
        void getById_existingUser_returnsUserResponse() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));

            // when
            UserResponse response = userService.getById(1L);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getUsername()).isEqualTo("testuser");
            verify(userGateway).findById(1L);
        }

        @Test
        @DisplayName("用户不存在时抛出 ResourceNotFoundException")
        void getById_nonExistingUser_throwsException() {
            // given
            when(userGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("99");
        }
    }

    // ==================== query 测试 ====================

    @Nested
    @DisplayName("query 查询用户列表")
    class QueryTests {

        @Test
        @DisplayName("返回所有用户")
        void query_noFilter_returnsAllUsers() {
            // given
            User user2 = createTestUser(2L, "user2", "user2@example.com");
            when(userGateway.findAll()).thenReturn(List.of(testUser, user2));

            UserQueryRequest request = new UserQueryRequest();
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<UserResponse> response = userService.query(request);

            // then
            assertThat(response.getItems()).hasSize(2);
            assertThat(response.getPagination().getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("按关键字过滤用户")
        void query_withKeyword_filtersUsers() {
            // given
            when(userGateway.findAll()).thenReturn(List.of(testUser));

            UserQueryRequest request = new UserQueryRequest();
            request.setKeyword("test");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<UserResponse> response = userService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("按状态过滤用户")
        void query_withStatus_filtersUsers() {
            // given
            when(userGateway.findAll()).thenReturn(List.of(testUser));

            UserQueryRequest request = new UserQueryRequest();
            request.setState(UserState.ACTIVE);
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<UserResponse> response = userService.query(request);

            // then
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getState()).isEqualTo(UserState.ACTIVE);
        }

        @Test
        @DisplayName("分页查询")
        void query_withPagination_returnsPagedUsers() {
            // given
            List<User> users = new ArrayList<>();
            for (long i = 1; i <= 25; i++) {
                users.add(createTestUser(i, "user" + i, "user" + i + "@example.com"));
            }
            when(userGateway.findAll()).thenReturn(users);

            UserQueryRequest request = new UserQueryRequest();
            request.setPage(2);
            request.setLimit(10);

            // when
            PageResponse<UserResponse> response = userService.query(request);

            // then
            assertThat(response.getItems()).hasSize(10);
            assertThat(response.getPagination().getPage()).isEqualTo(2);
            assertThat(response.getPagination().getLimit()).isEqualTo(10);
            assertThat(response.getPagination().getTotal()).isEqualTo(25);
            assertThat(response.getPagination().getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("关键字不匹配时返回空列表")
        void query_noMatch_returnsEmptyList() {
            // given
            when(userGateway.findAll()).thenReturn(List.of(testUser));

            UserQueryRequest request = new UserQueryRequest();
            request.setKeyword("nonexistent");
            request.setPage(1);
            request.setLimit(20);

            // when
            PageResponse<UserResponse> response = userService.query(request);

            // then
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getPagination().getTotal()).isEqualTo(0);
        }
    }

    // ==================== update 测试 ====================

    @Nested
    @DisplayName("update 更新用户")
    class UpdateTests {

        @Test
        @DisplayName("更新用户名成功")
        void update_validUsername_updatesUser() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));
            when(userGateway.save(any(User.class))).thenReturn(testUser);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername("updateduser");

            // when
            UserResponse response = userService.update(1L, request);

            // then
            assertThat(response).isNotNull();
            verify(userGateway).findById(1L);
            verify(userGateway).save(testUser);
        }

        @Test
        @DisplayName("更新邮箱成功")
        void update_validEmail_updatesUser() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));
            when(userGateway.save(any(User.class))).thenReturn(testUser);

            UserUpdateRequest request = new UserUpdateRequest();
            request.setEmail("updated@example.com");

            // when
            userService.update(1L, request);

            // then
            verify(userGateway).save(testUser);
        }

        @Test
        @DisplayName("用户不存在时抛出异常")
        void update_nonExistingUser_throwsException() {
            // given
            when(userGateway.findById(99L)).thenReturn(Optional.empty());

            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername("updateduser");

            // when & then
            assertThatThrownBy(() -> userService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== delete 测试 ====================

    @Nested
    @DisplayName("delete 删除用户")
    class DeleteTests {

        @Test
        @DisplayName("删除用户成功（软删除）")
        void delete_existingUser_softDeletes() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));
            when(userGateway.save(any(User.class))).thenReturn(testUser);

            // when
            userService.delete(1L);

            // then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userGateway).save(userCaptor.capture());
            assertThat(testUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("删除不存在的用户抛出异常")
        void delete_nonExistingUser_throwsException() {
            // given
            when(userGateway.findById(99L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== updateStatus 测试 ====================

    @Nested
    @DisplayName("updateStatus 更新用户状态")
    class UpdateStatusTests {

        @Test
        @DisplayName("更新用户状态成功")
        void updateStatus_validRequest_updatesStatus() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));
            when(userGateway.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UserStateUpdateRequest request = new UserStateUpdateRequest();
            request.setState(UserState.INACTIVE);

            // when
            UserResponse response = userService.updateState(1L, request);

            // then
            assertThat(testUser.getState()).isEqualTo(UserState.INACTIVE);
            verify(userGateway).save(testUser);
        }

        @Test
        @DisplayName("更新不存在用户的状态抛出异常")
        void updateState_nonExistingUser_throwsException() {
            // given
            when(userGateway.findById(99L)).thenReturn(Optional.empty());

            UserStateUpdateRequest request = new UserStateUpdateRequest();
            request.setState(UserState.LOCKED);

            // when & then
            assertThatThrownBy(() -> userService.updateState(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== assignRoles 测试 ====================

    @Nested
    @DisplayName("assignRoles 分配用户角色")
    class AssignRolesTests {

        @Test
        @DisplayName("分配角色成功")
        void assignRoles_validRequest_assignsRoles() {
            // given
            when(userGateway.findById(1L)).thenReturn(Optional.of(testUser));
            when(userGateway.save(any(User.class))).thenReturn(testUser);

            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of("ADMIN"));

            // when
            UserResponse response = userService.assignRoles(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(testUser.getRole()).isEqualTo("ADMIN");
            verify(userGateway).save(testUser);
        }

        @Test
        @DisplayName("分配角色到不存在的用户抛出异常")
        void assignRoles_nonExistingUser_throwsException() {
            // given
            when(userGateway.findById(99L)).thenReturn(Optional.empty());

            UserRoleAssignRequest request = new UserRoleAssignRequest();
            request.setRoleCodes(List.of("ADMIN"));

            // when & then
            assertThatThrownBy(() -> userService.assignRoles(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ==================== 辅助方法 ====================

    private User createTestUser(Long id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setPhone("13800138000");
        user.setState(UserState.ACTIVE);
        user.setEmailVerified(false);
        user.setRole("USER");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        return user;
    }
}
