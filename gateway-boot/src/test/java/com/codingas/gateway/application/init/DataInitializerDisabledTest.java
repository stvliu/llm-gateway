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
package com.codingas.gateway.application.init;

import com.codingas.gateway.iam.application.ApplicationGateway;
import com.codingas.gateway.iam.user.UserGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 demo-data-enabled=false 时 DataInitializer 的行为。
 *
 * <p>使用独立的 profile 确保不启用演示数据。</p>
 */
@SpringBootTest
@ActiveProfiles({"test"})
@Transactional
class DataInitializerDisabledTest {

    @Autowired
    private UserGateway userGateway;

    @Autowired
    private ApplicationGateway applicationGateway;

    @Autowired
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("admin 应始终创建")
    void adminShouldBeCreated() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("admin").isPresent());
    }

    @Test
    @DisplayName("不应创建演示用户")
    void shouldNotCreateDemoUsers() {
        dataInitializer.run();
        assertTrue(userGateway.findByUsername("test1").isEmpty());
    }

    @Test
    @DisplayName("不应创建演示应用")
    void shouldNotCreateDemoApplications() {
        dataInitializer.run();
        assertNull(applicationGateway.findByCode("dev"));
    }
}
