package com.codingas.gateway.infrastructure.actuator;

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
        assertThat(state.lastError()).isNull();
    }

    @Test
    @DisplayName("withSuccess 返回 UP 状态并累加连续成功")
    void withSuccess_returnsUp() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withSuccess();

        assertThat(state.status()).isEqualTo(Status.UP);
        assertThat(state.consecutiveFailures()).isZero();
        assertThat(state.consecutiveSuccesses()).isEqualTo(1);
        assertThat(state.lastError()).isNull();
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("withFailure 累加连续失败但不改变状态")
    void withFailure_recordsError() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withFailure("connection refused");

        assertThat(state.status()).isEqualTo(Status.UNKNOWN);
        assertThat(state.consecutiveFailures()).isEqualTo(1);
        assertThat(state.lastError()).isEqualTo("connection refused");
        assertThat(state.lastRequestTime()).isNotNull();
    }

    @Test
    @DisplayName("连续失败累加")
    void withFailure_accumulates() {
        var state = ProviderHealthState.initial("openai")
                .withFailure("err1")
                .withFailure("err2");

        assertThat(state.consecutiveFailures()).isEqualTo(2);
        assertThat(state.lastError()).isEqualTo("err2");
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
    @DisplayName("withProbe 更新检测时间")
    void withProbe_updatesCheckTime() {
        var initial = ProviderHealthState.initial("openai");
        var state = initial.withProbe(Status.UP);

        assertThat(state.lastCheckTime()).isNotNull();
        assertThat(state.status()).isEqualTo(Status.UP);
    }
}
