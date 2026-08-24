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
package com.codingas.gateway.resilience.retry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ServiceUnavailableStrategy 单元测试
 */
@DisplayName("ServiceUnavailableStrategy 测试")
class ServiceUnavailableStrategyTest {

    @Test
    @DisplayName("maxAttempts 取服务不可用配置")
    void maxAttempts_usesServiceUnavailableConfig() {
        GatewayRetryProperties properties = new GatewayRetryProperties();
        properties.getServiceUnavailable().setMaxAttempts(3);

        ServiceUnavailableStrategy strategy = new ServiceUnavailableStrategy(properties);

        assertThat(strategy.maxAttempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("calculateDelay 返回固定退避间隔")
    void calculateDelay_returnsFixedBackoff() {
        GatewayRetryProperties properties = new GatewayRetryProperties();
        properties.getServiceUnavailable().setBackoffFixed(5000);

        ServiceUnavailableStrategy strategy = new ServiceUnavailableStrategy(properties);

        assertThat(strategy.calculateDelay(1)).isEqualTo(5000);
        assertThat(strategy.calculateDelay(2)).isEqualTo(5000);
    }
}
