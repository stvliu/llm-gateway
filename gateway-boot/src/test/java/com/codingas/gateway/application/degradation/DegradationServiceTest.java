package com.codingas.gateway.application.degradation;

import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;
import com.codingas.gateway.infrastructure.actuator.ProviderHealthTracker;
import com.codingas.gateway.infrastructure.actuator.ProviderHealthState;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 智能降级服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("智能降级服务测试")
class DegradationServiceTest {

    @Mock
    private ProviderHealthTracker healthTracker;

    @Mock
    private DomainEventPublisher eventPublisher;

    private DegradationProperties properties;
    private MeterRegistry meterRegistry;
    private DegradationServiceImpl degradationService;

    @BeforeEach
    void setUp() {
        properties = new DegradationProperties();
        meterRegistry = new SimpleMeterRegistry();

        DegradationProperties.DegradationChain chain = new DegradationProperties.DegradationChain();
        chain.setPrimary("gpt-4o");
        chain.setFallbacks(List.of("claude-sonnet-4", "gpt-4o-mini"));
        chain.getRecovery().setSuccessThreshold(1);
        properties.setChains(List.of(chain));
    }

    @Nested
    @DisplayName("降级逻辑")
    class DegradeTests {

        @Test
        @DisplayName("主模型不可用时返回第一个备选")
        void degrade_primaryUnhealthy_returnsFirstFallback() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isEqualTo("claude-sonnet-4");
        }

        @Test
        @DisplayName("所有备选均不可用时返回 null")
        void degrade_allFallbacksUnavailable_returnsNull() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(new ProviderHealthState("claude-sonnet-4", Status.DOWN, null, 5, 0, "error"));
            when(healthTracker.getCachedStatus("gpt-4o-mini"))
                    .thenReturn(new ProviderHealthState("gpt-4o-mini", Status.DOWN, null, 3, 0, "error"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("未配置降级链的模型返回 null")
        void degrade_noChain_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            String result = degradationService.degrade("unknown-model", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("禁用了降级时返回 null")
        void degrade_disabled_returnsNull() {
            properties.setEnabled(false);
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("降级时发布 DegradationEvent")
        void degrade_publishesEvent() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isEqualTo("claude-sonnet-4");
            assertThat(meterRegistry.counter("gateway.degradation.triggered",
                    "from_model", "gpt-4o",
                    "to_model", "claude-sonnet-4",
                    "reason", "UPSTREAM_ERROR").count()).isPositive();
        }
    }

    @Nested
    @DisplayName("循环引用校验")
    class CircularReferenceTests {

        @Test
        @DisplayName("自身循环引用抛出异常")
        void selfCircular_throwsException() {
            DegradationProperties.DegradationChain selfChain = new DegradationProperties.DegradationChain();
            selfChain.setPrimary("gpt-4o");
            selfChain.setFallbacks(List.of("gpt-4o"));
            properties.setChains(List.of(selfChain));

            assertThatThrownBy(() -> new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("循环引用");
        }

        @Test
        @DisplayName("双向循环引用抛出异常")
        void bidirectionalCircular_throwsException() {
            DegradationProperties.DegradationChain chain1 = new DegradationProperties.DegradationChain();
            chain1.setPrimary("gpt-4o");
            chain1.setFallbacks(List.of("claude-sonnet-4"));

            DegradationProperties.DegradationChain chain2 = new DegradationProperties.DegradationChain();
            chain2.setPrimary("claude-sonnet-4");
            chain2.setFallbacks(List.of("gpt-4o"));

            properties.setChains(List.of(chain1, chain2));

            assertThatThrownBy(() -> new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("循环引用");
        }
    }

    @Nested
    @DisplayName("恢复检查")
    class RecoveryTests {

        @Test
        @DisplayName("连续成功达到阈值后标记恢复")
        void recoveryCheck_consecutiveSuccesses_marksRecovered() {
            when(healthTracker.getCachedStatus("gpt-4o"))
                    .thenReturn(new ProviderHealthState("gpt-4o", Status.UP, null, 0, 3, null));
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            // 触发降级
            degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
            assertThat(degradationService.canRecover("gpt-4o")).isFalse();

            // 恢复检查 — gpt-4o 已 UP，应该恢复
            degradationService.recoveryCheck();
            assertThat(degradationService.canRecover("gpt-4o")).isTrue();

            assertThat(meterRegistry.counter("gateway.degradation.recovered",
                    "model", "gpt-4o").count()).isPositive();
        }

        @Test
        @DisplayName("健康状态仍为 DOWN 时不恢复")
        void recoveryCheck_stillDown_notRecovered() {
            when(healthTracker.getCachedStatus("gpt-4o"))
                    .thenReturn(new ProviderHealthState("gpt-4o", Status.DOWN, null, 5, 0, "error"));
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher);

            degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
            degradationService.recoveryCheck();

            assertThat(degradationService.canRecover("gpt-4o")).isFalse();
        }
    }
}
