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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.channel.ChannelState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ChannelState 枚举单元测试
 *
 * <p>覆盖 5 状态的 isRoutable、isTerminal、canTransitionTo 全路径。</p>
 */
@DisplayName("ChannelState 枚举测试")
class ChannelStateTest {

    @Nested
    @DisplayName("isRoutable 测试")
    class IsRoutableTests {

        @Test
        @DisplayName("ACTIVE 可路由")
        void active_isRoutable() {
            assertThat(ChannelState.ACTIVE.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED 可路由（低优先级）")
        void deprecated_isRoutable() {
            assertThat(ChannelState.DEPRECATED.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("PENDING 不可路由")
        void pending_isNotRoutable() {
            assertThat(ChannelState.PENDING.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 不可路由")
        void suspended_isNotRoutable() {
            assertThat(ChannelState.SUSPENDED.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("RETIRED 不可路由")
        void retired_isNotRoutable() {
            assertThat(ChannelState.RETIRED.isRoutable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTerminal 测试")
    class IsTerminalTests {

        @Test
        @DisplayName("RETIRED 是终态")
        void retired_isTerminal() {
            assertThat(ChannelState.RETIRED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("ACTIVE 不是终态")
        void active_isNotTerminal() {
            assertThat(ChannelState.ACTIVE.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PENDING 不是终态")
        void pending_isNotTerminal() {
            assertThat(ChannelState.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 不是终态")
        void suspended_isNotTerminal() {
            assertThat(ChannelState.SUSPENDED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED 不是终态")
        void deprecated_isNotTerminal() {
            assertThat(ChannelState.DEPRECATED.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("canTransitionTo 测试")
    class CanTransitionToTests {

        // PENDING
        @Test
        @DisplayName("PENDING → ACTIVE 合法")
        void pendingToActive_valid() {
            assertThat(ChannelState.PENDING.canTransitionTo(ChannelState.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("PENDING → SUSPENDED 非法")
        void pendingToSuspended_invalid() {
            assertThat(ChannelState.PENDING.canTransitionTo(ChannelState.SUSPENDED)).isFalse();
        }

        @Test
        @DisplayName("PENDING → DEPRECATED 非法")
        void pendingToDeprecated_invalid() {
            assertThat(ChannelState.PENDING.canTransitionTo(ChannelState.DEPRECATED)).isFalse();
        }

        @Test
        @DisplayName("PENDING → RETIRED 非法")
        void pendingToRetired_invalid() {
            assertThat(ChannelState.PENDING.canTransitionTo(ChannelState.RETIRED)).isFalse();
        }

        // ACTIVE
        @Test
        @DisplayName("ACTIVE → SUSPENDED 合法")
        void activeToSuspended_valid() {
            assertThat(ChannelState.ACTIVE.canTransitionTo(ChannelState.SUSPENDED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → DEPRECATED 合法")
        void activeToDeprecated_valid() {
            assertThat(ChannelState.ACTIVE.canTransitionTo(ChannelState.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → PENDING 非法")
        void activeToPending_invalid() {
            assertThat(ChannelState.ACTIVE.canTransitionTo(ChannelState.PENDING)).isFalse();
        }

        @Test
        @DisplayName("ACTIVE → RETIRED 非法（必须经过 DEPRECATED）")
        void activeToRetired_invalid() {
            assertThat(ChannelState.ACTIVE.canTransitionTo(ChannelState.RETIRED)).isFalse();
        }

        // SUSPENDED
        @Test
        @DisplayName("SUSPENDED → ACTIVE 合法")
        void suspendedToActive_valid() {
            assertThat(ChannelState.SUSPENDED.canTransitionTo(ChannelState.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → DEPRECATED 合法")
        void suspendedToDeprecated_valid() {
            assertThat(ChannelState.SUSPENDED.canTransitionTo(ChannelState.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → RETIRED 合法（允许直达）")
        void suspendedToRetired_valid() {
            assertThat(ChannelState.SUSPENDED.canTransitionTo(ChannelState.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → PENDING 非法")
        void suspendedToPending_invalid() {
            assertThat(ChannelState.SUSPENDED.canTransitionTo(ChannelState.PENDING)).isFalse();
        }

        // DEPRECATED
        @Test
        @DisplayName("DEPRECATED → RETIRED 合法")
        void deprecatedToRetired_valid() {
            assertThat(ChannelState.DEPRECATED.canTransitionTo(ChannelState.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED → ACTIVE 非法")
        void deprecatedToActive_invalid() {
            assertThat(ChannelState.DEPRECATED.canTransitionTo(ChannelState.ACTIVE)).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED → SUSPENDED 非法")
        void deprecatedToSuspended_invalid() {
            assertThat(ChannelState.DEPRECATED.canTransitionTo(ChannelState.SUSPENDED)).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED → PENDING 非法")
        void deprecatedToPending_invalid() {
            assertThat(ChannelState.DEPRECATED.canTransitionTo(ChannelState.PENDING)).isFalse();
        }

        // RETIRED
        @Test
        @DisplayName("RETIRED → 任何状态均非法")
        void retiredToAny_invalid() {
            for (ChannelState target : ChannelState.values()) {
                assertThat(ChannelState.RETIRED.canTransitionTo(target)).isFalse();
            }
        }
    }
}
