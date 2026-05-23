package com.codingas.gateway.infrastructure.actuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderRegistryHealthIndicator 测试")
class ProviderRegistryHealthIndicatorTest {

    @Mock
    private ProviderHealthTracker tracker;

    private ProviderRegistryHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        indicator = new ProviderRegistryHealthIndicator(tracker);
    }

    @Test
    @DisplayName("所有 Provider UP 时健康状态为 UP")
    void allProvidersUp_returnsUp() {
        when(tracker.getAllStatuses()).thenReturn(List.of(
                new ProviderHealthState("openai", Status.UP, null, 0, 1, null),
                new ProviderHealthState("anthropic", Status.UP, null, 0, 2, null)
        ));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        var openaiDetail = (Map<?, ?>) health.getDetails().get("openai");
        assertThat(openaiDetail.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("部分 Provider DOWN 时健康状态为 UP（降级但不宕机）")
    void partialDown_returnsUp() {
        when(tracker.getAllStatuses()).thenReturn(List.of(
                new ProviderHealthState("openai", Status.UP, null, 0, 1, null),
                new ProviderHealthState("anthropic", Status.DOWN, null, 3, 0, "timeout")
        ));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        var anthropicDetail = (Map<?, ?>) health.getDetails().get("anthropic");
        assertThat(anthropicDetail.get("status")).isEqualTo("DOWN");
        assertThat(anthropicDetail.get("lastError")).isEqualTo("timeout");
    }

    @Test
    @DisplayName("所有 Provider DOWN 时健康状态为 DOWN")
    void allDown_returnsDown() {
        when(tracker.getAllStatuses()).thenReturn(List.of(
                new ProviderHealthState("openai", Status.DOWN, null, 3, 0, "timeout"),
                new ProviderHealthState("anthropic", Status.DOWN, null, 5, 0, "refused")
        ));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("无 Provider 时健康状态为 DOWN")
    void noProviders_returnsDown() {
        when(tracker.getAllStatuses()).thenReturn(List.of());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("Provider UNKNOWN 状态视为非健康")
    void unknownProviders_treatedAsDown() {
        when(tracker.getAllStatuses()).thenReturn(List.of(
                ProviderHealthState.initial("openai")
        ));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}