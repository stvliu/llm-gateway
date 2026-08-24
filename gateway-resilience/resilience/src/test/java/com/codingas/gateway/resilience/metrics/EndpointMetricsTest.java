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
package com.codingas.gateway.resilience.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EndpointMetrics 单元测试
 */
@DisplayName("EndpointMetrics 测试")
class EndpointMetricsTest {

    @Test
    @DisplayName("初始统计为零")
    void initialState_allZero() {
        EndpointMetrics metrics = new EndpointMetrics();

        assertThat(metrics.getActive()).isZero();
        assertThat(metrics.getTotalCalls()).isZero();
        assertThat(metrics.getTotalDuration()).isZero();
        assertThat(metrics.getFailedCalls()).isZero();
        assertThat(metrics.getAverageDuration()).isZero();
        assertThat(metrics.getFailureRate()).isZero();
    }

    @Test
    @DisplayName("beginCall 使活跃数加一")
    void beginCall_incrementsActive() {
        EndpointMetrics metrics = new EndpointMetrics();

        metrics.beginCall();
        metrics.beginCall();

        assertThat(metrics.getActive()).isEqualTo(2);
    }

    @Test
    @DisplayName("成功调用更新调用数与耗时，失败数不变")
    void endCall_success_updatesCallsAndDuration() {
        EndpointMetrics metrics = new EndpointMetrics();

        metrics.beginCall();
        metrics.endCall(150, true);

        assertThat(metrics.getActive()).isZero();
        assertThat(metrics.getTotalCalls()).isEqualTo(1);
        assertThat(metrics.getTotalDuration()).isEqualTo(150);
        assertThat(metrics.getFailedCalls()).isZero();
    }

    @Test
    @DisplayName("失败调用同时更新失败数与失败率")
    void endCall_failure_updatesFailedAndFailureRate() {
        EndpointMetrics metrics = new EndpointMetrics();

        metrics.beginCall();
        metrics.endCall(50, false);
        metrics.beginCall();
        metrics.endCall(50, true);

        assertThat(metrics.getFailedCalls()).isEqualTo(1);
        assertThat(metrics.getTotalCalls()).isEqualTo(2);
        assertThat(metrics.getTotalDuration()).isEqualTo(100);
        assertThat(metrics.getAverageDuration()).isEqualTo(50.0);
        assertThat(metrics.getFailureRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("失败率按失败次数除以总调用数")
    void failureRate_multipleFailures_computed() {
        EndpointMetrics metrics = new EndpointMetrics();
        metrics.beginCall();
        metrics.endCall(10, false);
        metrics.beginCall();
        metrics.endCall(10, false);
        metrics.beginCall();
        metrics.endCall(10, true);

        assertThat(metrics.getFailureRate()).isEqualTo(2.0 / 3.0);
    }
}
