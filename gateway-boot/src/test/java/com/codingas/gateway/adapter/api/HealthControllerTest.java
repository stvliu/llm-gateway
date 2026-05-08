package com.codingas.gateway.adapter.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HealthController 单元测试
 *
 * <p>Controller 现在直接返回业务对象，由 ApiResponseWrapperAdvice 自动包装。</p>
 */
@DisplayName("HealthController 测试")
class HealthControllerTest {

    @Test
    @DisplayName("健康检查返回正常状态")
    void health_returnsUpStatus() {
        // given
        HealthController controller = new HealthController();

        // when
        Map<String, Object> result = controller.health();

        // then
        assertThat(result).containsEntry("status", "UP");
        assertThat(result).containsEntry("service", "llm-gateway");
        assertThat(result).containsKey("timestamp");
    }
}
