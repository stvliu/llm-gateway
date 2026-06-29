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
    @DisplayName("按 priority 升序输出完整列表不收敛")
    void keepsAllInstances_sortedByPriorityAscending() {
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

        // 不收敛：保留全部 3 个；按 priority 升序：100,100,200 → [mi1, mi3, mi2]
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 3L, 2L);
    }

    @Test
    @DisplayName("主备 priority 不同时输出完整列表 [主,备] 不丢备")
    void primaryAndBackup_differentPriority_keepsFullList() {
        ModelInstance primary = new ModelInstance();
        primary.setId(1L);
        primary.setPriority(1);
        ModelInstance backup = new ModelInstance();
        backup.setId(2L);
        backup.setPriority(2);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(primary, backup), request);

        // 主备不丢：返回 [主,备]，priority 升序（L1 故障转移前提）
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
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
    @DisplayName("priority 为 null 时使用默认值 100 并参与完整排序")
    void nullPriority_usesDefault() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setPriority(null);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setPriority(200);

        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        // null 回退 100，与 priority=200 一起完整排序，不收敛 → [mi1(100), mi2(200)]
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
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
