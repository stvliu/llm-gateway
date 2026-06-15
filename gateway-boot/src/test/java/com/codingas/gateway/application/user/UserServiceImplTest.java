package com.codingas.gateway.application.user;

import com.codingas.gateway.application.user.dto.*;
import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.domain.iam.enums.UserState;
import com.codingas.gateway.common.exception.DuplicateResourceException;
import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.infrastructure.config.SecurityConfig.PasswordEncoder;
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
