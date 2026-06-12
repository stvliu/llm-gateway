package com.codingas.gateway.domain.supply.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Channel.State 枚举单元测试
 *
 * <p>覆盖 5 状态的 isRoutable、isTerminal、canTransitionTo 全路径。</p>
 */
@DisplayName("Channel.State 枚举测试")
class ChannelStateTest {

    @Nested
    @DisplayName("isRoutable 测试")
    class IsRoutableTests {

        @Test
        @DisplayName("ACTIVE 可路由")
        void active_isRoutable() {
            assertThat(Channel.State.ACTIVE.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED 可路由（低优先级）")
        void deprecated_isRoutable() {
            assertThat(Channel.State.DEPRECATED.isRoutable()).isTrue();
        }

        @Test
        @DisplayName("PENDING 不可路由")
        void pending_isNotRoutable() {
            assertThat(Channel.State.PENDING.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 不可路由")
        void suspended_isNotRoutable() {
            assertThat(Channel.State.SUSPENDED.isRoutable()).isFalse();
        }

        @Test
        @DisplayName("RETIRED 不可路由")
        void retired_isNotRoutable() {
            assertThat(Channel.State.RETIRED.isRoutable()).isFalse();
        }
    }

    @Nested
    @DisplayName("isTerminal 测试")
    class IsTerminalTests {

        @Test
        @DisplayName("RETIRED 是终态")
        void retired_isTerminal() {
            assertThat(Channel.State.RETIRED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("ACTIVE 不是终态")
        void active_isNotTerminal() {
            assertThat(Channel.State.ACTIVE.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("PENDING 不是终态")
        void pending_isNotTerminal() {
            assertThat(Channel.State.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 不是终态")
        void suspended_isNotTerminal() {
            assertThat(Channel.State.SUSPENDED.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED 不是终态")
        void deprecated_isNotTerminal() {
            assertThat(Channel.State.DEPRECATED.isTerminal()).isFalse();
        }
    }

    @Nested
    @DisplayName("canTransitionTo 测试")
    class CanTransitionToTests {

        // PENDING
        @Test
        @DisplayName("PENDING → ACTIVE 合法")
        void pendingToActive_valid() {
            assertThat(Channel.State.PENDING.canTransitionTo(Channel.State.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("PENDING → SUSPENDED 非法")
        void pendingToSuspended_invalid() {
            assertThat(Channel.State.PENDING.canTransitionTo(Channel.State.SUSPENDED)).isFalse();
        }

        @Test
        @DisplayName("PENDING → DEPRECATED 非法")
        void pendingToDeprecated_invalid() {
            assertThat(Channel.State.PENDING.canTransitionTo(Channel.State.DEPRECATED)).isFalse();
        }

        @Test
        @DisplayName("PENDING → RETIRED 非法")
        void pendingToRetired_invalid() {
            assertThat(Channel.State.PENDING.canTransitionTo(Channel.State.RETIRED)).isFalse();
        }

        // ACTIVE
        @Test
        @DisplayName("ACTIVE → SUSPENDED 合法")
        void activeToSuspended_valid() {
            assertThat(Channel.State.ACTIVE.canTransitionTo(Channel.State.SUSPENDED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → DEPRECATED 合法")
        void activeToDeprecated_valid() {
            assertThat(Channel.State.ACTIVE.canTransitionTo(Channel.State.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("ACTIVE → PENDING 非法")
        void activeToPending_invalid() {
            assertThat(Channel.State.ACTIVE.canTransitionTo(Channel.State.PENDING)).isFalse();
        }

        @Test
        @DisplayName("ACTIVE → RETIRED 非法（必须经过 DEPRECATED）")
        void activeToRetired_invalid() {
            assertThat(Channel.State.ACTIVE.canTransitionTo(Channel.State.RETIRED)).isFalse();
        }

        // SUSPENDED
        @Test
        @DisplayName("SUSPENDED → ACTIVE 合法")
        void suspendedToActive_valid() {
            assertThat(Channel.State.SUSPENDED.canTransitionTo(Channel.State.ACTIVE)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → DEPRECATED 合法")
        void suspendedToDeprecated_valid() {
            assertThat(Channel.State.SUSPENDED.canTransitionTo(Channel.State.DEPRECATED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → RETIRED 合法（允许直达）")
        void suspendedToRetired_valid() {
            assertThat(Channel.State.SUSPENDED.canTransitionTo(Channel.State.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("SUSPENDED → PENDING 非法")
        void suspendedToPending_invalid() {
            assertThat(Channel.State.SUSPENDED.canTransitionTo(Channel.State.PENDING)).isFalse();
        }

        // DEPRECATED
        @Test
        @DisplayName("DEPRECATED → RETIRED 合法")
        void deprecatedToRetired_valid() {
            assertThat(Channel.State.DEPRECATED.canTransitionTo(Channel.State.RETIRED)).isTrue();
        }

        @Test
        @DisplayName("DEPRECATED → ACTIVE 非法")
        void deprecatedToActive_invalid() {
            assertThat(Channel.State.DEPRECATED.canTransitionTo(Channel.State.ACTIVE)).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED → SUSPENDED 非法")
        void deprecatedToSuspended_invalid() {
            assertThat(Channel.State.DEPRECATED.canTransitionTo(Channel.State.SUSPENDED)).isFalse();
        }

        @Test
        @DisplayName("DEPRECATED → PENDING 非法")
        void deprecatedToPending_invalid() {
            assertThat(Channel.State.DEPRECATED.canTransitionTo(Channel.State.PENDING)).isFalse();
        }

        // RETIRED
        @Test
        @DisplayName("RETIRED → 任何状态均非法")
        void retiredToAny_invalid() {
            for (Channel.State target : Channel.State.values()) {
                assertThat(Channel.State.RETIRED.canTransitionTo(target)).isFalse();
            }
        }
    }
}
