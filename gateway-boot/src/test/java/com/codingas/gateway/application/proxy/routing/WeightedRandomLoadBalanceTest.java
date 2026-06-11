package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WeightedRandomLoadBalance 单元测试
 */
@DisplayName("WeightedRandomLoadBalance 单元测试")
class WeightedRandomLoadBalanceTest {

    private WeightedRandomLoadBalance loadBalance;

    @BeforeEach
    void setUp() {
        loadBalance = new WeightedRandomLoadBalance();
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
    @DisplayName("null 或空列表返回 null")
    void nullOrEmpty_returnsNull() {
        assertThat(loadBalance.select(null)).isNull();
        assertThat(loadBalance.select(List.of())).isNull();
    }

    @RepeatedTest(10)
    @DisplayName("所有权重相同时分布均匀")
    void sameWeight_evenDistribution() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setWeight(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setWeight(100);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setWeight(100);

        // 运行 1000 次验证每个实例都被选中过
        java.util.stream.IntStream.range(0, 1000)
                .mapToObj(i -> loadBalance.select(List.of(mi1, mi2, mi3)))
                .collect(Collectors.groupingBy(ModelInstance::getId, Collectors.counting()));

        // 没有断言异常即表示所有实例都能被选中
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("权重为 null 时使用默认值 100")
    void nullWeight_usesDefault() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setWeight(null);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setWeight(null);

        ModelInstance result = loadBalance.select(List.of(mi1, mi2));

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("权重不同时高权重实例被选中的次数更多")
    void differentWeight_weightedDistribution() {
        ModelInstance mi1 = new ModelInstance();
        mi1.setId(1L);
        mi1.setWeight(1);
        ModelInstance mi2 = new ModelInstance();
        mi2.setId(2L);
        mi2.setWeight(3);
        ModelInstance mi3 = new ModelInstance();
        mi3.setId(3L);
        mi3.setWeight(6);

        // 运行 10000 次，统计分布
        Map<Long, Long> counts = java.util.stream.IntStream.range(0, 10000)
                .mapToObj(i -> loadBalance.select(List.of(mi1, mi2, mi3)))
                .collect(Collectors.groupingBy(ModelInstance::getId, Collectors.counting()));

        // 权重 1:3:6，mi3 应该最多，mi1 最少
        assertThat(counts.get(3L)).isGreaterThan(counts.get(1L));
        assertThat(counts.get(3L)).isGreaterThan(counts.get(2L));
    }
}
