package com.codingas.gateway.adapter.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HealthController 单元测试
 */
@DisplayName("HealthController 测试")
class HealthControllerTest {

    @Test
    @DisplayName("健康检查返回正常状态")
    void health_returnsUpStatus() {
        // given
        HealthController controller = new HealthController();

        // when
        var result = controller.health();

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("status", "UP");
        assertThat(result.getData()).containsEntry("service", "llm-gateway");
        assertThat(result.getData()).containsKey("timestamp");
    }
}
