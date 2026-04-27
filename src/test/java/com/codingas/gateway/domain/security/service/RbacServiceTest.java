package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.domain.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RbacService 单元测试
 */
@DisplayName("RbacService 权限控制测试")
class RbacServiceTest {

    private RbacService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacService();
    }

    @Nested
    @DisplayName("hasPermission 权限检查")
    class HasPermissionTests {

        @Test
        @DisplayName("user 为 null 时返回 false")
        void hasPermission_nullUser_returnsFalse() {
            boolean result = rbacService.hasPermission(null, "anyResource", "anyAction");
            assertThat(result).isFalse();
        }

        @Nested
        @DisplayName("ADMIN 角色")
        class AdminRoleTests {

            @Test
            @DisplayName("ADMIN 可以访问任意资源")
            void hasPermission_adminRole_returnsTrue() {
                User admin = createUser(User.UserRole.ADMIN);

                boolean result = rbacService.hasPermission(admin, "sensitive_data", "delete");

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("ADMIN 可以执行任意操作")
            void hasPermission_adminRole_anyAction_returnsTrue() {
                User admin = createUser(User.UserRole.ADMIN);

                boolean deleteResult = rbacService.hasPermission(admin, "resource", "delete");
                boolean writeResult = rbacService.hasPermission(admin, "resource", "write");
                boolean readResult = rbacService.hasPermission(admin, "resource", "read");

                assertThat(deleteResult).isTrue();
                assertThat(writeResult).isTrue();
                assertThat(readResult).isTrue();
            }
        }

        @Nested
        @DisplayName("USER 角色")
        class UserRoleTests {

            @Test
            @DisplayName("USER 可以访问资源")
            void hasPermission_userRole_returnsTrue() {
                User user = createUser(User.UserRole.USER);

                // 当前实现 checkUserPermission 总是返回 true
                boolean result = rbacService.hasPermission(user, "anyResource", "anyAction");

                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("READONLY 角色")
        class ReadonlyRoleTests {

            @Test
            @DisplayName("READONLY 只能执行 read 操作")
            void hasPermission_readonlyRole_readAction_returnsTrue() {
                User readonly = createUser(User.UserRole.READONLY);

                boolean result = rbacService.hasPermission(readonly, "anyResource", "read");

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("READONLY 不能执行 write 操作")
            void hasPermission_readonlyRole_writeAction_returnsFalse() {
                User readonly = createUser(User.UserRole.READONLY);

                boolean result = rbacService.hasPermission(readonly, "anyResource", "write");

                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("READONLY 不能执行 delete 操作")
            void hasPermission_readonlyRole_deleteAction_returnsFalse() {
                User readonly = createUser(User.UserRole.READONLY);

                boolean result = rbacService.hasPermission(readonly, "anyResource", "delete");

                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("READONLY 不能执行 admin 操作")
            void hasPermission_readonlyRole_adminAction_returnsFalse() {
                User readonly = createUser(User.UserRole.READONLY);

                boolean result = rbacService.hasPermission(readonly, "admin_panel", "manage");

                assertThat(result).isFalse();
            }
        }
    }

    /**
     * 创建测试用 User 实体
     */
    private User createUser(User.UserRole role) {
        return new User()
                .setUserCode("test_user_code")
                .setUserName("Test User")
                .setEmail("test@example.com")
                .setRole(role)
                .setStatus(User.UserStatus.ACTIVE);
    }
}
