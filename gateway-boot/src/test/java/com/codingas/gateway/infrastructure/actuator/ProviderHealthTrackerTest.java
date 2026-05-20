package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.infrastructure.proxy.gateway.rpc.LLMAdapter;
import com.codingas.gateway.infrastructure.proxy.gateway.rpc.AdapterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderHealthTracker 测试")
class ProviderHealthTrackerTest {

    @Mock
    private AdapterRegistry adapterRegistry;

    @Mock
    private LLMAdapter openaiAdapter;

    @Mock
    private LLMAdapter anthropicAdapter;

    private ProviderHealthProperties properties;
    private ProviderHealthTracker tracker;

    @BeforeEach
    void setUp() {
        properties = new ProviderHealthProperties();
        properties.setFailureThreshold(3);
        properties.setSuccessThreshold(2);
        properties.setStaleThreshold(Duration.ofSeconds(300));
        properties.setProbeTimeout(Duration.ofSeconds(10));

        lenient().when(openaiAdapter.getProviderCode()).thenReturn("openai");
        lenient().when(openaiAdapter.checkConnection()).thenReturn(true);
        lenient().when(anthropicAdapter.getProviderCode()).thenReturn("anthropic");
        lenient().when(anthropicAdapter.checkConnection()).thenReturn(true);

        lenient().when(adapterRegistry.getAllAdapters()).thenReturn(List.of(openaiAdapter, anthropicAdapter));
        lenient().when(adapterRegistry.getAdapter("openai")).thenReturn(Optional.of(openaiAdapter));
        lenient().when(adapterRegistry.getAdapter("anthropic")).thenReturn(Optional.of(anthropicAdapter));

        tracker = new ProviderHealthTracker(adapterRegistry, properties);
    }

    @Test
    @DisplayName("初始缓存状态为 UNKNOWN")
    void initialCachedStatus_isUnknown() {
        var status = tracker.getCachedStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    @DisplayName("getStatus 触发探测后返回 UP")
    void getStatus_triggersProbe_returnsUp() {
        var status = tracker.getStatus("openai");
        assertThat(status.status()).isEqualTo(Status.UP);
        verify(openaiAdapter).checkConnection();
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
