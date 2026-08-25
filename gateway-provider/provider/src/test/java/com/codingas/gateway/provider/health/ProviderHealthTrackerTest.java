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

import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderHealthTracker 测试")
class ProviderHealthTrackerTest {

    @Mock
    private UpstreamClientRegistry upstreamClientRegistry;

    @Mock
    private UpstreamClient openaiRepository;

    @Mock
    private UpstreamClient anthropicRepository;

    private ProviderHealthProperties properties;
    private ProviderHealthTracker tracker;

    @BeforeEach
    void setUp() {
        properties = new ProviderHealthProperties();
        properties.setFailureThreshold(3);
        properties.setSuccessThreshold(2);
        properties.setStaleThreshold(Duration.ofSeconds(300));
        properties.setProbeTimeout(Duration.ofSeconds(10));

        lenient().when(upstreamClientRegistry.getSupportedProtocols()).thenReturn(List.of("openai", "anthropic"));

        tracker = new ProviderHealthTracker(upstreamClientRegistry, properties);
    }

    @Test
    @DisplayName("初始缓存状态为 UNKNOWN")
    void initialCachedStatus_isUnknown() {
        var status = tracker.getCachedStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("getStatus 返回当前状态（被动推断模式）")
    void getStatus_returnsCurrentState() {
        var status = tracker.getStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("记录成功请求后状态为 UP")
    void recordSuccess_statusIsUp() {
        tracker.recordRequestResult("openai", true, null);

        var status = tracker.getCachedStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UP);
        assertThat(status.consecutiveSuccesses()).isEqualTo(1);
    }

    @Test
    @DisplayName("连续失败未达阈值时状态不变")
    void consecutiveFailures_belowThreshold_statusUnchanged() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        var status = tracker.getCachedStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UNKNOWN);
        assertThat(status.consecutiveFailures()).isEqualTo(2);
    }

    @Test
    @DisplayName("连续失败达到阈值后状态为 DOWN")
    void consecutiveFailures_reachesDown() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        var status = tracker.getCachedStatus("openai");
        assertThat(status.status()).isEqualTo(Status.DOWN);
        assertThat(status.consecutiveFailures()).isEqualTo(3);
    }

    @Test
    @DisplayName("DOWN 后单次成功不恢复 UP（需连续成功 successThreshold 次）")
    void recovery_singleSuccess_staysDown() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        assertThat(tracker.getCachedStatus("openai").status()).isEqualTo(Status.DOWN);

        tracker.recordRequestResult("openai", true, null);

        assertThat(tracker.getCachedStatus("openai").status()).isEqualTo(Status.DOWN);
        assertThat(tracker.getCachedStatus("openai").consecutiveSuccesses()).isEqualTo(1);
    }

    @Test
    @DisplayName("DOWN 后连续成功 successThreshold 次恢复 UP")
    void recovery_consecutiveSuccesses_reachesUp() {
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");
        tracker.recordRequestResult("openai", false, "timeout");

        assertThat(tracker.getCachedStatus("openai").status()).isEqualTo(Status.DOWN);

        tracker.recordRequestResult("openai", true, null);
        tracker.recordRequestResult("openai", true, null);

        assertThat(tracker.getCachedStatus("openai").status()).isEqualTo(Status.UP);
    }

    @Test
    @DisplayName("getAllStatuses 返回所有 Provider 状态")
    void getAllStatuses_returnsAll() {
        var all = tracker.getAllStatuses();
        assertThat(all).hasSize(2);
        assertThat(all.stream().map(ProviderHealthState::providerCode))
                .containsExactlyInAnyOrder("openai", "anthropic");
    }

    @Test
    @DisplayName("至少一个 Provider UP 时 hasHealthyProvider 为 true")
    void hasHealthyProvider_atLeastOneUp() {
        tracker.recordRequestResult("openai", true, null);

        assertThat(tracker.hasHealthyProvider()).isTrue();
    }

    @Test
    @DisplayName("所有 Provider DOWN 时 hasHealthyProvider 为 false")
    void hasHealthyProvider_allDown() {
        for (int i = 0; i < 3; i++) {
            tracker.recordRequestResult("openai", false, "err");
            tracker.recordRequestResult("anthropic", false, "err");
        }

        assertThat(tracker.hasHealthyProvider()).isFalse();
    }
}