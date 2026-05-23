package com.codingas.gateway.infrastructure.actuator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProviderHealthTracker 集成测试")
class ProviderHealthTrackerIntegrationTest {

    @Autowired
    private ProviderHealthTracker tracker;

    @Autowired
    private ProviderHealthProperties properties;

    @Test
    @DisplayName("Spring 容器正确注入 ProviderHealthTracker")
    void tracker_isInjected() {
        assertThat(tracker).isNotNull();
    }

    @Test
    @DisplayName("配置属性正确加载")
    void properties_areLoaded() {
        assertThat(properties.getFailureThreshold()).isEqualTo(3);
        assertThat(properties.getSuccessThreshold()).isEqualTo(2);
        assertThat(properties.getStaleThreshold().getSeconds()).isEqualTo(300);
    }

    @Test
    @DisplayName("getAllStatuses 返回已注册的 Provider")
    void getAllStatuses_returnsRegisteredProviders() {
        var statuses = tracker.getAllStatuses();
        assertThat(statuses).isNotEmpty();
    }

    @Test
    @DisplayName("记录成功请求后状态为 UP")
    void recordSuccess_statusIsUp() {
        var providers = tracker.getAllStatuses();
        if (!providers.isEmpty()) {
            String code = providers.getFirst().providerCode();
            tracker.recordRequestResult(code, true, null);
            assertThat(tracker.getCachedStatus(code).status()).isEqualTo(Status.UP);
        }
    }
}
