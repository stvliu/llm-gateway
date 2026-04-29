package com.codingas.gateway.domain.security.service;

import com.codingas.gateway.common.enums.UserRole;
import com.codingas.gateway.common.enums.UserStatus;
import com.codingas.gateway.domain.security.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RbacDomainService 单元测试
 */
@DisplayName("RbacDomainService 权限控制测试")
class RbacDomainServiceTest {

    private RbacDomainService rbacService;

    @BeforeEach
    void setUp() {
        rbacService = new RbacDomainService();
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
        @DisplayName("有效用户")
        class ValidUserTests {

            @Test
            @DisplayName("有效用户可以访问任意资源")
            void hasPermission_validUser_returnsTrue() {
                User user = createUser();

                boolean result = rbacService.hasPermission(user, "sensitive_data", "delete");

                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("有效用户可以执行任意操作")
            void hasPermission_validUser_anyAction_returnsTrue() {
                User user = createUser();

                boolean deleteResult = rbacService.hasPermission(user, "resource", "delete");
                boolean writeResult = rbacService.hasPermission(user, "resource", "write");
                boolean readResult = rbacService.hasPermission(user, "resource", "read");

                assertThat(deleteResult).isTrue();
                assertThat(writeResult).isTrue();
                assertThat(readResult).isTrue();
            }
        }
    }

    /**
     * 创建测试用 User 实体
     */
    private User createUser() {
        User user = new User();
        user.setUserCode("test_user_code");
        user.setUsername("Test User");
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
