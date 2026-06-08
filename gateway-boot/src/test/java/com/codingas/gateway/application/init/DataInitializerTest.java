package com.codingas.gateway.application.init;

import com.codingas.gateway.domain.iam.entity.User;
import com.codingas.gateway.domain.iam.gateway.UserGateway;
import com.codingas.gateway.domain.supply.gateway.ProviderGateway;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"test", "test-demo"})
@Transactional
class DataInitializerTest {

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private ProviderGateway providerGateway;

    @Autowired
    private TeamGateway teamGateway;

    @Autowired
    private DataInitializer dataInitializer;

    // ===== Phase 1: Admin 内置用户 =====

    @Test
    @DisplayName("run 后 admin 用户应存在")
    void adminUserShouldExist() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("admin").isPresent());
    }

    @Test
    @DisplayName("admin 用户应标记为 builtin 且邮箱正确")
    void adminUserShouldBeBuiltin() {
        User admin = userGateway.findByUsername("admin")
            .orElseThrow(() -> new AssertionError("admin 应存在"));
        assertTrue(admin.isBuiltin());
        assertEquals("admin@example.com", admin.getEmail());
    }

    @Test
    @DisplayName("admin 用户角色应为 ADMIN")
    void adminUserShouldHaveAdminRole() {
        User admin = userGateway.findByUsername("admin")
            .orElseThrow(() -> new AssertionError("admin 应存在"));
        assertEquals("ADMIN", admin.getRole());
    }

    // ===== Phase 3/4: Demo 数据 =====

    @Test
    @DisplayName("应创建 test1-test10 共 10 个演示用户")
    void shouldCreateTenDemoUsers() {
        dataInitializer.run();
        for (int i = 1; i <= 10; i++) {
            String username = "test" + i;
            assertTrue(userGateway.findByUsername(username).isPresent(),
                "演示用户 " + username + " 应存在");
        }
    }

    @Test
    @DisplayName("演示用户不应标记为 builtin")
    void demoUserShouldNotBeBuiltin() {
        dataInitializer.run();
        User test1 = userGateway.findByUsername("test1")
            .orElseThrow(() -> new AssertionError("test1 应存在"));
        assertFalse(test1.isBuiltin());
    }

    @Test
    @DisplayName("应创建 4 个演示团队（default, dev, product, openclaw）")
    void shouldCreateFourDemoTeams() {
        dataInitializer.run();
        assertTrue(teamGateway.existsByName("default"));
        assertTrue(teamGateway.existsByName("dev"));
        assertTrue(teamGateway.existsByName("product"));
        assertTrue(teamGateway.existsByName("openclaw"));
    }

    @Test
    @DisplayName("应存在供应商（由 BuiltinDataLoader 或后备逻辑创建）")
    void shouldHaveProviders() {
        dataInitializer.run();
        assertTrue(providerGateway.count() >= 6,
            "应有至少 6 个供应商（来自 BuiltinDataLoader 或后备逻辑）");
    }

    // ===== 幂等性 =====

    @Nested
    @DisplayName("幂等性")
    class Idempotency {

        @Test
        @DisplayName("重复调用不应创建重复的 admin")
        void repeatedRunShouldNotDuplicateAdmin() {
            dataInitializer.run();
            long adminCount1 = userGateway.findAll().stream()
                .filter(u -> "admin".equals(u.getUsername()))
                .count();
            dataInitializer.run();
            long adminCount2 = userGateway.findAll().stream()
                .filter(u -> "admin".equals(u.getUsername()))
                .count();
            assertEquals(adminCount1, adminCount2);
        }

        @Test
        @DisplayName("重复调用不应创建重复的演示用户")
        void repeatedRunShouldNotDuplicateDemoUsers() {
            dataInitializer.run();
            long count1 = userGateway.count();
            dataInitializer.run();
            long count2 = userGateway.count();
            assertEquals(count1, count2);
        }

        @Test
        @DisplayName("重复调用不应创建重复的团队")
        void repeatedRunShouldNotDuplicateTeams() {
            dataInitializer.run();
            long count1 = teamGateway.findAllActive().size();
            dataInitializer.run();
            long count2 = teamGateway.findAllActive().size();
            assertEquals(count1, count2);
        }
    }
}