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
package com.codingas.gateway.domain.supply.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelInstance.State 枚举单元测试
 *
 * <p>覆盖 5 状态的 isRoutable、isTerminal、canTransitionTo 全路径。</p>
 */
@DisplayName("ModelInstance.State 枚举测试")
class ModelInstanceStateTest {

    @Nested
    @DisplayName("isRoutable 测试")
    class IsRoutableTests {

        @Test
        @DisplayName("ACTIVE 可路由")
        void active_isRoutable() {
            assertThat(ModelInstance.State.ACTIVE.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED 可路由")
        void deprecated_isRoutable() {
            assertThat(ModelInstance.State.DEPRECATED.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("PENDING 不可路由")
        void pending_isNotRoutable() {
            assertThat(ModelInstance.State.PENDING.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 不可路由")
        void suspended_isNotRoutable() {
            assertThat(ModelInstance.State.SUSPENDED.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("RETIRED 不可路由")
        void retired_isNotRoutable() {
            assertThat(ModelInstance.State.RETIRED.isRoutable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTerminal 测试")
    class IsTerminalTests {

        @Test
        @DisplayName("RETIRED 是终态")
        void retired_isTerminal() {
            assertThat(ModelInstance.State.RETIRED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("非 RETIRED 都不是终态")
        void nonRetired_isNotTerminal() {
            for (ModelInstance.State state : ModelInstance.State.values()) {
                if (state != ModelInstance.State.RETIRED) {
                    assertThat(state.isTerminal()).isFalse();
                }
            }
        }
    }

    @Nested
    @DisplayName("canTransitionTo 测试")
    class CanTransitionToTests {

        // PENDING
        @Test
        @DisplayName("PENDING → ACTIVE 合法")
        void pendingToActive_valid() {
            assertThat(ModelInstance.State.PENDING.canTransitionTo(ModelInstance.State.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("PENDING → 非 ACTIVE 非法")
        void pendingToNonActive_invalid() {
            for (ModelInstance.State target : ModelInstance.State.values()) {
                if (target != ModelInstance.State.ACTIVE) {
                    assertThat(ModelInstance.State.PENDING.canTransitionTo(target)).isFalse();
                }
            }
        }

        // ACTIVE
        @Test
        @DisplayName("ACTIVE → SUSPENDED 合法")
        void activeToSuspended_valid() {
            assertThat(ModelInstance.State.ACTIVE.canTransitionTo(ModelInstance.State.SUSPENDED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → DEPRECATED 合法")
        void activeToDeprecated_valid() {
            assertThat(ModelInstance.State.ACTIVE.canTransitionTo(ModelInstance.State.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → RETIRED 非法")
        void activeToRetired_invalid() {
            assertThat(ModelInstance.State.ACTIVE.canTransitionTo(ModelInstance.State.RETIRED)).isFalse();
        }

        // SUSPENDED
        @Test
        @DisplayName("SUSPENDED → ACTIVE 合法")
        void suspendedToActive_valid() {
            assertThat(ModelInstance.State.SUSPENDED.canTransitionTo(ModelInstance.State.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → DEPRECATED 合法")
        void suspendedToDeprecated_valid() {
            assertThat(ModelInstance.State.SUSPENDED.canTransitionTo(ModelInstance.State.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → RETIRED 合法（允许直达）")
        void suspendedToRetired_valid() {
            assertThat(ModelInstance.State.SUSPENDED.canTransitionTo(ModelInstance.State.RETIRED)).isTrue();
        }

        // DEPRECATED
        @Test
        @DisplayName("DEPRECATED → RETIRED 合法")
        void deprecatedToRetired_valid() {
            assertThat(ModelInstance.State.DEPRECATED.canTransitionTo(ModelInstance.State.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED → 非 RETIRED 非法")
        void deprecatedToNonRetired_invalid() {
            for (ModelInstance.State target : ModelInstance.State.values()) {
                if (target != ModelInstance.State.RETIRED) {
                    assertThat(ModelInstance.State.DEPRECATED.canTransitionTo(target)).isFalse();
                }
            }
        }

        // RETIRED
        @Test
        @DisplayName("RETIRED → 任何状态均非法")
        void retiredToAny_invalid() {
            for (ModelInstance.State target : ModelInstance.State.values()) {
                assertThat(ModelInstance.State.RETIRED.canTransitionTo(target)).isFalse();
            }
        }
    }
}
