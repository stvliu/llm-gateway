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
package com.codingas.gateway.provider.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProviderHealthState 测试")
class ProviderHealthStateTest {

    @Test
    @DisplayName("创建初始状态为 UNKNOWN")
    void createInitial_returnsUnknown() {
        var state = ProviderHealthState.initial("openai");

        assertThat(state.providerCode()).isEqualTo("openai");
        assertThat(state.status()).isEqualTo(Status.UNKNOWN);
        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.consecutiveSuccesses()).isZero();
        assertThat(state.lastErrorMessage()).isNull();
    }

    @Test
    @DisplayName("withSuccess 返回 UP 状态并累加连续成功")
    void withSuccess_returnsUp() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withSuccess();

        assertThat(state.status()).isEqualTo(Status.UP);
        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.consecutiveSuccesses()).isEqualTo(1);
        assertThat(state.lastErrorMessage()).isNull();
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("withFailure 设置 DOWN 状态并累加连续失败")
    void withFailure_recordsError() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withFailure("connection refused");

        assertThat(state.status()).isEqualTo(Status.DOWN);
        assertThat(state.consecutiveFailures()).isEqualTo(1);
        assertThat(state.lastErrorMessage()).isEqualTo("connection refused");
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("连续失败累加")
    void withFailure_accumulates() {
        var state = ProviderHealthState.initial("openai")
                .withFailure("err1")
                .withFailure("err2");

        assertThat(state.consecutiveFailures()).isEqualTo(2);
        assertThat(state.lastErrorMessage()).isEqualTo("err2");
    }

    @Test
    @DisplayName("成功后重置连续失败计数")
    void withSuccess_resetsFailures() {
        var state = ProviderHealthState.initial("openai")
                .withFailure("err1")
                .withFailure("err2")
                .withSuccess();

        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.consecutiveSuccesses()).isEqualTo(1);
        assertThat(state.status()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("失败后重置连续成功计数")
    void withFailure_resetsSuccesses() {
        var state = ProviderHealthState.initial("openai")
                .withSuccess()
                .withSuccess()
                .withFailure("err1");

        assertThat(state.consecutiveSuccesses()).isZero();
        assertThat(state.consecutiveFailures()).isEqualTo(1);
    }

    @Test
    @DisplayName("isStale 无请求时间时返回 true")
    void isStale_noRequestTime_returnsTrue() {
        var state = ProviderHealthState.initial("openai");

        assertThat(state.isStale(java.time.Duration.ofSeconds(300))).isTrue();
    }

    @Test
    @DisplayName("isStale 未过期时返回 false")
    void isStale_notExpired_returnsFalse() {
        var state = ProviderHealthState.initial("openai").withSuccess();

        assertThat(state.isStale(java.time.Duration.ofSeconds(300))).isFalse();
    }
}