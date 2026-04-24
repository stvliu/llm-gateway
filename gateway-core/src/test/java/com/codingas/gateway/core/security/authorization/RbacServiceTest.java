package com.codingas.gateway.core.security.authorization;

import com.codingas.gateway.core.domain.entity.User;
import com.codingas.gateway.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * RbacService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RbacService 测试")
class RbacServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RbacService rbacService;

    private User adminUser;
    private User normalUser;
    private User readonlyUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUserCode("admin001");
        adminUser.setRole(User.UserRole.ADMIN);

        normalUser = new User();
        normalUser.setId(2L);
        normalUser.setUserCode("user001");
        normalUser.setRole(User.UserRole.USER);

        readonlyUser = new User();
        readonlyUser.setId(3L);
        readonlyUser.setUserCode("readonly001");
        readonlyUser.setRole(User.UserRole.READONLY);
    }

    @Nested
    @DisplayName("hasPermission() 测试")
    class HasPermissionTests {

        @Test
        @DisplayName("ADMIN 角色拥有所有权限")
        void admin_hasAllPermissions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThat(rbacService.hasPermission(1L, "api:call")).isTrue();
            assertThat(rbacService.hasPermission(1L, "api:read")).isTrue();
            assertThat(rbacService.hasPermission(1L, "api:write")).isTrue();
            assertThat(rbacService.hasPermission(1L, "admin:dashboard")).isTrue();
            assertThat(rbacService.hasPermission(1L, "system:config")).isTrue();
        }

        @Test
        @DisplayName("USER 角色只能访问 api:* 权限")
        void user_hasOnlyApiPermissions() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.hasPermission(2L, "api:call")).isTrue();
            assertThat(rbacService.hasPermission(2L, "api:read")).isTrue();
            assertThat(rbacService.hasPermission(2L, "api:write")).isTrue();
            assertThat(rbacService.hasPermission(2L, "api:delete")).isTrue();

            assertThat(rbacService.hasPermission(2L, "admin:dashboard")).isFalse();
            assertThat(rbacService.hasPermission(2L, "system:config")).isFalse();
        }

        @Test
        @DisplayName("READONLY 角色只能访问 api:read 权限")
        void readonly_hasOnlyReadPermission() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(readonlyUser));

            assertThat(rbacService.hasPermission(3L, "api:read")).isTrue();

            assertThat(rbacService.hasPermission(3L, "api:call")).isFalse();
            assertThat(rbacService.hasPermission(3L, "api:write")).isFalse();
            assertThat(rbacService.hasPermission(3L, "admin:dashboard")).isFalse();
        }

        @Test
        @DisplayName("null 参数返回 false")
        void nullParameters_returnFalse() {
            assertThat(rbacService.hasPermission(null, "api:read")).isFalse();
            assertThat(rbacService.hasPermission(1L, null)).isFalse();
            assertThat(rbacService.hasPermission(null, null)).isFalse();
        }

        @Test
        @DisplayName("用户不存在返回 false")
        void userNotFound_returnFalse() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(rbacService.hasPermission(99L, "api:read")).isFalse();
        }
    }

    @Nested
    @DisplayName("hasRole() 测试")
    class HasRoleTests {

        @Test
        @DisplayName("正确识别用户角色")
        void correctRoleIdentified() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));
            when(userRepository.findById(3L)).thenReturn(Optional.of(readonlyUser));

            assertThat(rbacService.hasRole(1L, User.UserRole.ADMIN)).isTrue();
            assertThat(rbacService.hasRole(2L, User.UserRole.USER)).isTrue();
            assertThat(rbacService.hasRole(3L, User.UserRole.READONLY)).isTrue();
        }

        @Test
        @DisplayName("角色不匹配返回 false")
        void roleMismatch_returnFalse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.hasRole(1L, User.UserRole.USER)).isFalse();
            assertThat(rbacService.hasRole(2L, User.UserRole.ADMIN)).isFalse();
            assertThat(rbacService.hasRole(2L, User.UserRole.READONLY)).isFalse();
        }

        @Test
        @DisplayName("null 参数返回 false")
        void nullParameters_returnFalse() {
            assertThat(rbacService.hasRole(null, User.UserRole.ADMIN)).isFalse();
            assertThat(rbacService.hasRole(1L, null)).isFalse();
        }

        @Test
        @DisplayName("用户不存在返回 false")
        void userNotFound_returnFalse() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(rbacService.hasRole(99L, User.UserRole.ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("isAdmin() 测试")
    class IsAdminTests {

        @Test
        @DisplayName("ADMIN 用户返回 true")
        void adminUser_returnsTrue() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThat(rbacService.isAdmin(1L)).isTrue();
        }

        @Test
        @DisplayName("USER 用户返回 false")
        void normalUser_returnsFalse() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.isAdmin(2L)).isFalse();
        }

        @Test
        @DisplayName("READONLY 用户返回 false")
        void readonlyUser_returnsFalse() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(readonlyUser));

            assertThat(rbacService.isAdmin(3L)).isFalse();
        }

        @Test
        @DisplayName("用户不存在返回 false")
        void userNotFound_returnsFalse() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(rbacService.isAdmin(99L)).isFalse();
        }
    }

    @Nested
    @DisplayName("getUserPermissions() 测试")
    class GetUserPermissionsTests {

        @Test
        @DisplayName("ADMIN 角色返回所有权限")
        void admin_returnsAllPermissions() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThat(rbacService.getUserPermissions(1L)).containsExactly("*");
        }

        @Test
        @DisplayName("USER 角色返回 api:call 和 api:read")
        void user_returnsApiPermissions() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.getUserPermissions(2L)).containsExactlyInAnyOrder("api:call", "api:read");
        }

        @Test
        @DisplayName("READONLY 角色只返回 api:read")
        void readonly_returnsReadPermission() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(readonlyUser));

            assertThat(rbacService.getUserPermissions(3L)).containsExactly("api:read");
        }

        @Test
        @DisplayName("null userId 返回空集合")
        void nullUserId_returnsEmptySet() {
            assertThat(rbacService.getUserPermissions(null)).isEmpty();
        }

        @Test
        @DisplayName("用户不存在返回空集合")
        void userNotFound_returnsEmptySet() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThat(rbacService.getUserPermissions(99L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("canAccessModel() 测试")
    class CanAccessModelTests {

        @Test
        @DisplayName("管理员可以访问所有模型")
        void admin_canAccessAllModels() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThat(rbacService.canAccessModel(1L, "gpt-4", null)).isTrue();
            assertThat(rbacService.canAccessModel(1L, "claude-3-opus", null)).isTrue();
            assertThat(rbacService.canAccessModel(1L, "gpt-4", "[\"gpt-4\"]")).isTrue();
            assertThat(rbacService.canAccessModel(1L, "unknown-model", "[\"gpt-4\"]")).isTrue();
        }

        @Test
        @DisplayName("白名单为空时允许访问所有模型")
        void whitelistNull_orEmpty_allowsAll() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.canAccessModel(2L, "gpt-4", null)).isTrue();
            assertThat(rbacService.canAccessModel(2L, "claude-3-opus", "")).isTrue();
            assertThat(rbacService.canAccessModel(2L, "any-model", "   ")).isTrue();
        }

        @Test
        @DisplayName("用户在白名单中允许访问")
        void modelInWhitelist_allowsAccess() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(normalUser));

            assertThat(rbacService.canAccessModel(2L, "gpt-4", "[\"gpt-4\", \"gpt-3.5\"]")).isTrue();
            assertThat(rbacService.canAccessModel(2L, "claude-3-opus", "[\"claude-3-opus\"]")).isTrue();
        }

        @Test
        @DisplayName("用户不在白名单中拒绝访问")
        void modelNotInWhitelist_deniesAccess() {
            when(userRepository.findById(3L)).thenReturn(Optional.of(readonlyUser));

            assertThat(rbacService.canAccessModel(3L, "gpt-4", "[\"gpt-3.5\"]")).isFalse();
            assertThat(rbacService.canAccessModel(3L, "claude-3-opus", "[\"gpt-4\"]")).isFalse();
        }
    }
}
