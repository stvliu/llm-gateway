package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RouterChain 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouterChain 单元测试")
class RouterChainTest {

    private RouterChain routerChain;

    @BeforeEach
    void setUp() {
        routerChain = new RouterChain(List.of());
    }

    @Test
    @DisplayName("空 Router 列表时返回原列表")
    void emptyRouters_returnsOriginalList() {
        List<ModelInstance> instances = List.of(new ModelInstance());
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);

        List<ModelInstance> result = routerChain.filter(instances, request);

        assertThat(result).isSameAs(instances);
    }

    @Test
    @DisplayName("非强制 Router 返回空时跳过")
    void nonForceRouter_empty_skipped() {
        Router nonForceRouter = (instances, req) -> List.of(); // isForce=false by default
        RouterChain chain = new RouterChain(List.of(nonForceRouter));

        List<ModelInstance> instances = List.of(new ModelInstance());
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);

        List<ModelInstance> result = chain.filter(instances, request);

        assertThat(result).isSameAs(instances);
    }

    @Test
    @DisplayName("强制 Router 返回空时直接返回空")
    void forceRouter_empty_returnsEmpty() {
        Router forceRouter = new Router() {
            @Override
            public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
                return List.of();
            }
            @Override
            public boolean isForce() { return true; }
        };
        RouterChain chain = new RouterChain(List.of(forceRouter));

        List<ModelInstance> instances = List.of(new ModelInstance());
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);

        List<ModelInstance> result = chain.filter(instances, request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Router 按 @Order 排序执行")
    void routers_executedInOrder() {
        @Order(200)
        class RouterB implements Router {
            @Override
            public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
                return instances.stream().filter(mi -> mi.getWeight() != null && mi.getWeight() > 50).toList();
            }
        }

        @Order(100)
        class RouterA implements Router {
            @Override
            public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
                return instances.stream().filter(mi -> mi.getWeight() != null).toList();
            }
        }

        RouterChain chain = new RouterChain(List.of(new RouterB(), new RouterA()));

        ModelInstance mi1 = new ModelInstance();
        mi1.setWeight(100);
        ModelInstance mi2 = new ModelInstance();
        mi2.setWeight(30);
        ModelInstance mi3 = new ModelInstance();
        mi3.setWeight(null);

        List<ModelInstance> result = chain.filter(List.of(mi1, mi2, mi3),
                new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED));

        // RouterA (100) 先执行：过滤掉 weight=null 的 → [mi1, mi2]
        // RouterB (200) 后执行：过滤掉 weight<=50 的 → [mi1]
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getWeight()).isEqualTo(100);
    }
}
