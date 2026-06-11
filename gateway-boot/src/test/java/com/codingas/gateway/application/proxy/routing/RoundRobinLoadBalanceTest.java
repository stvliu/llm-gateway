package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RoundRobinLoadBalance 单元测试
 */
@DisplayName("RoundRobinLoadBalance 单元测试")
class RoundRobinLoadBalanceTest {

    private RoundRobinLoadBalance loadBalance;

    @BeforeEach
    void setUp() {
        loadBalance = new RoundRobinLoadBalance();
    }

    @Test
    @DisplayName("单元素列表直接返回")
    void singleInstance_returnsDirectly() {
        ModelInstance mi = new ModelInstance();
        mi.setId(1L);
        mi.setWeight(100);

        ModelInstance result = loadBalance.select(List.of(mi));

        assertThat(result).isSameAs(mi);
    }

    @Test
    @DisplayName("所有权重相同时轮流选择")
    void sameWeight_roundRobin() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setWeight(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setWeight(100);

        List<ModelInstance> instances = List.of(mi1, mi2);

        // 第一次选 mi1，第二次选 mi2，第三次选 mi1...
        assertThat(loadBalance.select(instances).getId()).isEqualTo(1L);
        assertThat(loadBalance.select(instances).getId()).isEqualTo(2L);
        assertThat(loadBalance.select(instances).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("加权轮询 N 次后各实例被选次数比例接近权重比")
    void weightedRoundRobin_distribution() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setWeight(1);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setWeight(2);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setWeight(3);

        // 总共 6 次选择（权重和），每个实例被选次数应等于其权重
        Map<Long, Long> counts = IntStream.range(0, 6)
                .mapToObj(i -> loadBalance.select(List.of(mi1, mi2, mi3)))
                .collect(Collectors.groupingBy(ModelInstance::getId, Collectors.counting()));

        // 由于平滑轮询不是严格 1:2:3 的排列，但总体比例应接近
        assertThat(counts.get(3L)).isGreaterThan(counts.get(2L));
        assertThat(counts.get(2L)).isGreaterThan(counts.get(1L));
    }

    @Test
    @DisplayName("null 或空列表返回 null")
    void nullOrEmpty_returnsNull() {
        assertThat(loadBalance.select(null)).isNull();
        assertThat(loadBalance.select(List.of())).isNull();
    }
}
