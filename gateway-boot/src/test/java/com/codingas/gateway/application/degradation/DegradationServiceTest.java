package com.codingas.gateway.application.degradation;

import com.codingas.gateway.application.proxy.failover.ErrorClassifier;
import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.resilience.entity.ResilienceProfile;
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
    private ErrorClassifier errorClassifier;
    private DegradationServiceImpl degradationService;

    @BeforeEach
    void setUp() {
        properties = new DegradationProperties();
        meterRegistry = new SimpleMeterRegistry();
        errorClassifier = new ErrorClassifier();

        DegradationProperties.DegradationChain chain = new DegradationProperties.DegradationChain();
        chain.setPrimary("gpt-4o");
        chain.setFallbacks(List.of("claude-sonnet-4", "gpt-4o-mini"));
        chain.getRecovery().setSuccessThreshold(1);
        properties.setChains(List.of(chain));
    }

    @Nested
    @DisplayName("画像门禁 L2 降级（Task 4.8）")
    class ProfileGatedDegradeTests {

        @Test
        @DisplayName("profile 关闭 L2 降级时返回 null")
        void degrade_profileDisabledL2_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            ResilienceProfile profile = profile(true, false, 5);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, profile);

            assertThat(result).as("画像关闭 enableL2ModelDegradation，degrade 应返回 null 不降级").isNull();
        }

        @Test
        @DisplayName("profile 开启 L2 降级时返回 fallback")
        void degrade_profileEnabledL2_returnsFallback() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            ResilienceProfile profile = profile(true, true, 5);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, profile);

            assertThat(result).as("画像开启 L2，UNKNOWN_ERROR 判为 L2 降级，应返回 fallback").isEqualTo("claude-sonnet-4");
        }

        @Test
        @DisplayName("degradationMaxDepth=0 禁用降级返回 null")
        void degrade_maxDepthZero_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            // maxDepth=0 表示禁用降级
            ResilienceProfile profile = profile(true, true, 0);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, profile);

            assertThat(result).as("degradationMaxDepth=0 禁用降级，应返回 null").isNull();
        }

        @Test
        @DisplayName("degradationMaxDepth 限制备选遍历深度：超深度健康备选不被触达")
        void degrade_maxDepthLimitsFallbacks() {
            // 两个备选：claude-sonnet-4（DOWN）、gpt-4o-mini（健康）
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(new ProviderHealthState("claude-sonnet-4", Status.DOWN, null, 5, 0, "error"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            // maxDepth=1：只允许尝试第 1 个备选（claude-sonnet-4，DOWN），不应触达第 2 个健康备选
            // 第 1 个备选 DOWN → 深度内备选耗尽 → degrade 抛 ALL_MODELS_DEGRADED（既有契约，3.6 技术债）
            ResilienceProfile profile = profile(true, true, 1);

            assertThatThrownBy(() -> degradationService.degrade("gpt-4o", ProviderErrorType.UNKNOWN_ERROR, profile))
                    .isInstanceOf(com.codingas.gateway.domain.supply.exception.ProviderException.class)
                    .hasMessageContaining("ALL_MODELS_DEGRADED");
            // 关键断言：第 2 个健康备选 gpt-4o-mini 未被查询（深度门禁生效）
            org.mockito.Mockito.verify(healthTracker, org.mockito.Mockito.never()).getCachedStatus("gpt-4o-mini");
        }

        @Test
        @DisplayName("按 errorType 分流：L1 类错误（UPSTREAM_ERROR）不触发模型降级返回 null")
        void degrade_l1ErrorType_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            ResilienceProfile profile = profile(true, true, 5);

            // UPSTREAM_ERROR 经 ErrorClassifier 判为 L1（换渠道），非 L2 不应触发模型降级
            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR, profile);

            assertThat(result).as("L1 类错误应换渠道而非换模型，degrade 返回 null").isNull();
        }

        @Test
        @DisplayName("按 errorType 分流：NONE 类错误（INVALID_REQUEST）不触发模型降级返回 null")
        void degrade_noneErrorType_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            ResilienceProfile profile = profile(true, true, 5);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.INVALID_REQUEST, profile);

            assertThat(result).as("请求级错误换哪都无效，degrade 返回 null").isNull();
        }

        @Test
        @DisplayName("profile 为 null 时回退到无门禁逻辑（不阻断降级）")
        void degrade_nullProfile_fallbackToUngated() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            // profile=null：向后兼容，走无门禁旧逻辑（深度用 properties.maxChainDepth，不按 errorType 分流）
            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR, null);

            assertThat(result).as("profile=null 回退无门禁逻辑，UPSTREAM_ERROR 旧逻辑可降级").isEqualTo("claude-sonnet-4");
        }

        /** 构造测试用 ResilienceProfile 画像 */
        private ResilienceProfile profile(boolean enabled, boolean enableL2, int maxDepth) {
            ResilienceProfile p = new ResilienceProfile();
            p.setEnableL2ModelDegradation(enableL2);
            p.setDegradationMaxDepth(maxDepth);
            return p;
        }
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
                    meterRegistry, eventPublisher, errorClassifier);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isEqualTo("claude-sonnet-4");
        }

        @Test
        @DisplayName("所有备选均不可用时抛出 ProviderException")
        void degrade_allFallbacksUnavailable_throwsException() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(new ProviderHealthState("claude-sonnet-4", Status.DOWN, null, 5, 0, "error"));
            when(healthTracker.getCachedStatus("gpt-4o-mini"))
                    .thenReturn(new ProviderHealthState("gpt-4o-mini", Status.DOWN, null, 3, 0, "error"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            assertThatThrownBy(() -> degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR))
                    .isInstanceOf(com.codingas.gateway.domain.supply.exception.ProviderException.class)
                    .hasMessageContaining("ALL_MODELS_DEGRADED");
        }

        @Test
        @DisplayName("未配置降级链的模型返回 null")
        void degrade_noChain_returnsNull() {
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            String result = degradationService.degrade("unknown-model", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("禁用了降级时返回 null")
        void degrade_disabled_returnsNull() {
            properties.setEnabled(false);
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

            String result = degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("降级时发布 DegradationEvent")
        void degrade_publishesEvent() {
            when(healthTracker.getCachedStatus("claude-sonnet-4"))
                    .thenReturn(ProviderHealthState.initial("claude-sonnet-4"));
            degradationService = new DegradationServiceImpl(properties, healthTracker,
                    meterRegistry, eventPublisher, errorClassifier);

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
                    meterRegistry, eventPublisher, errorClassifier))
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
                    meterRegistry, eventPublisher, errorClassifier))
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
                    meterRegistry, eventPublisher, errorClassifier);

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
                    meterRegistry, eventPublisher, errorClassifier);

            degradationService.degrade("gpt-4o", ProviderErrorType.UPSTREAM_ERROR);
            degradationService.recoveryCheck();

            assertThat(degradationService.canRecover("gpt-4o")).isFalse();
        }
    }
}
