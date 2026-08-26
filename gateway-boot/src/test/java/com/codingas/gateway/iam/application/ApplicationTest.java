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
package com.codingas.gateway.iam.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import com.codingas.gateway.common.enums.FailureStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Application 根实体实体与 ApplicationState 枚举单元测试
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>{@link ApplicationState} 枚举值</li>
 *   <li>{@link Application} 实体全部字段可读写</li>
 * </ul>
 *
 * <p>Task 8：{@code resilienceProfileId} 退场，新增 {@code timeout}（承接原 ResilienceProfile.timeout，
 * 0 表示用渠道默认）。</p>
 */
@DisplayName("Application 根实体实体测试")
class ApplicationTest {

    @Nested
    @DisplayName("ApplicationState 枚举测试")
    class StateTests {

        @Test
        @DisplayName("枚举仅包含 ACTIVE 与 INACTIVE 两个值")
        void values_containsOnlyActiveAndInactive() {
            assertThat(ApplicationState.values())
                    .containsExactlyInAnyOrder(ApplicationState.ACTIVE, ApplicationState.INACTIVE);
        }
    }

    @Nested
    @DisplayName("Application 字段读写测试")
    class FieldReadWriteTests {

        @Test
        @DisplayName("无参构造器 + Setter 能读写全部 12 个字段（含 id 与审计字段）")
        void noArgsConstructor_andSetters_readWriteAllFields() {
            Application app = new Application();
            Instant now = Instant.now();

            app.setId(1L);
            app.setCode("app-002");
            app.setName("网关应用");
            app.setDescription("权限+行为双聚合");
            app.setState(ApplicationState.INACTIVE);
            app.setTimeout(60);
            app.setQuotaBudgetId(20L);
            app.setDashboardId(30L);
            app.setCreatedBy(999L);
            app.setCreatedAt(now);
            app.setUpdatedBy(888L);
            app.setUpdatedAt(now);

            assertThat(app.getId()).isEqualTo(1L);
            assertThat(app.getCode()).isEqualTo("app-002");
            assertThat(app.getName()).isEqualTo("网关应用");
            assertThat(app.getDescription()).isEqualTo("权限+行为双聚合");
            assertThat(app.getState()).isEqualTo(ApplicationState.INACTIVE);
            assertThat(app.getTimeout()).isEqualTo(60);
            assertThat(app.getQuotaBudgetId()).isEqualTo(20L);
            assertThat(app.getDashboardId()).isEqualTo(30L);
            assertThat(app.getCreatedBy()).isEqualTo(999L);
            assertThat(app.getCreatedAt()).isEqualTo(now);
            assertThat(app.getUpdatedBy()).isEqualTo(888L);
            assertThat(app.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("无参构造器初始状态：引用字段为 null，timeout 原始值 0")
        void noArgsConstructor_defaults() {
            Application app = new Application();

            assertThat(app.getId()).isNull();
            assertThat(app.getCode()).isNull();
            assertThat(app.getName()).isNull();
            assertThat(app.getDescription()).isNull();
            assertThat(app.getState()).isNull();
            // timeout 为原始 int，默认 0（表示用渠道默认）
            assertThat(app.getTimeout()).isEqualTo(0);
            assertThat(app.getQuotaBudgetId()).isNull();
            assertThat(app.getDashboardId()).isNull();
            assertThat(app.getCreatedBy()).isNull();
            assertThat(app.getCreatedAt()).isNull();
            assertThat(app.getUpdatedBy()).isNull();
            assertThat(app.getUpdatedAt()).isNull();
        }
    }
}
