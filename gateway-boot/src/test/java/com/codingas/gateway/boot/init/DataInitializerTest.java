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
package com.codingas.gateway.boot.init;

import com.codingas.gateway.boot.GatewayApplication;
import com.codingas.gateway.iam.application.ApplicationGateway;
import com.codingas.gateway.iam.user.User;
import com.codingas.gateway.iam.user.UserGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = GatewayApplication.class)
@ActiveProfiles({"test", "test-demo"})
@Transactional
class DataInitializerTest {

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private ProviderGateway providerGateway;

    @Autowired
    private ApplicationGateway applicationGateway;

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
    @DisplayName("应创建 4 个演示应用（default, dev, product, openclaw）")
    void shouldCreateFourDemoApplications() {
        dataInitializer.run();
        assertNotNull(applicationGateway.findByCode("default"));
        assertNotNull(applicationGateway.findByCode("dev"));
        assertNotNull(applicationGateway.findByCode("product"));
        assertNotNull(applicationGateway.findByCode("openclaw"));
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
        @DisplayName("重复调用不应创建重复的应用")
        void repeatedRunShouldNotDuplicateApplications() {
            dataInitializer.run();
            long count1 = applicationGateway.findAll().size();
            dataInitializer.run();
            long count2 = applicationGateway.findAll().size();
            assertEquals(count1, count2);
        }
    }
}
