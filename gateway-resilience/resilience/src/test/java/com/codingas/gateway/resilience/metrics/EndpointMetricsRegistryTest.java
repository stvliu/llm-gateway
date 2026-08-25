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
 * EndpointMetricsRegistry 单元测试
 */
@DisplayName("EndpointMetricsRegistry 测试")
class EndpointMetricsRegistryTest {

    private final EndpointMetricsRegistry registry = new EndpointMetricsRegistry();

    @Test
    @DisplayName("get 不存在时创建，再次获取复用同一实例")
    void get_createsAndReuses() {
        EndpointMetrics first = registry.get(1L);
        EndpointMetrics second = registry.get(1L);

        assertThat(first).isNotNull();
        assertThat(second).isSameAs(first);
    }

}
