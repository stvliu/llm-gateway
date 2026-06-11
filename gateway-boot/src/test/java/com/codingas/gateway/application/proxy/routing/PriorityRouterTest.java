package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PriorityRouter 单元测试
 */
@DisplayName("PriorityRouter 单元测试")
class PriorityRouterTest {

    private final PriorityRouter router = new PriorityRouter();

    @Test
    @DisplayName("只保留 priority 最小的组")
    void keepsMinPriorityGroup() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setPriority(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setPriority(200);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setPriority(100);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2, mi3), request);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("单 priority 组返回全部")
    void singlePriority_returnsAll() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setPriority(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setPriority(100);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("priority 为 null 时使用默认值 100")
    void nullPriority_usesDefault() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setPriority(null);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setPriority(200);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("空列表返回空")
    void emptyInput_returnsEmpty() {
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("isForce 返回 true")
    void isForce_returnsTrue() {
        assertThat(router.isForce()).isTrue();
    }
}
