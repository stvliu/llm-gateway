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
package com.codingas.gateway.protocol.transport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConnectivityTestResult 不可变对象测试
 *
 * <p>覆盖 success/failure 两个工厂方法生成的字段组合。</p>
 */
@DisplayName("ConnectivityTestResult 测试")
class ConnectivityTestResultTest {

    @Test
    @DisplayName("success 工厂：成功标记、渠道 ID、时延，无错误信息")
    void success_factory_populatesSuccessFields() {
        ConnectivityTestResult ok = ConnectivityTestResult.success(42L, 12L);

        assertThat(ok.success()).isTrue();
        assertThat(ok.channelId()).isEqualTo(42L);
        assertThat(ok.latencyMs()).isEqualTo(12L);
        assertThat(ok.errorMessage()).isNull();
    }

    @Test
    @DisplayName("failure 工厂：失败标记、渠道 ID、错误信息，时延为 0")
    void failure_factory_populatesFailureFields() {
        ConnectivityTestResult fail = ConnectivityTestResult.failure(7L, "boom");

        assertThat(fail.success()).isFalse();
        assertThat(fail.channelId()).isEqualTo(7L);
        assertThat(fail.errorMessage()).isEqualTo("boom");
        assertThat(fail.latencyMs()).isZero();
    }
}
