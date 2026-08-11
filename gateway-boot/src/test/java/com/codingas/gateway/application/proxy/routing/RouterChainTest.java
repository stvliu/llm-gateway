/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * RouterChain 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RouterChain 单元测试")
class RouterChainTest {

    private RouterChain routerChain;

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @Mock
    private EndpointResolver endpointResolver;

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

    @Test
    @DisplayName("真实路由器按 @Order 排序：Permission→Health→Priority→LoadBalance")
    void routers_executedInOrder_realRoutersSortedByOrder() throws Exception {
        // Permission/LoadBalance 用桩 Router 占 @Order 槽位；Health/Priority 用真实实现以驱动 @Order 修正
        @Order(100)
        class PermissionStub implements Router {
            @Override
            public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
                return instances;
            }
        }
        @Order(9999)
        class LoadBalanceStub implements Router {
            @Override
            public List<ModelInstance> filter(List<ModelInstance> instances, RoutingRequest request) {
                return instances;
            }
        }

        HealthRouter health = new HealthRouter(circuitBreakerManager, endpointResolver);
        PriorityRouter priority = new PriorityRouter();
        // 故意乱序传入，验证 RouterChain 按 @Order 升序排序
        RouterChain chain = new RouterChain(List.of(
                new LoadBalanceStub(), priority, health, new PermissionStub()));

        List<Router> sorted = readRouters(chain);

        assertThat(sorted).extracting(r -> r.getClass().getSimpleName())
                .containsExactly("PermissionStub", "HealthRouter", "PriorityRouter", "LoadBalanceStub");
    }

    @Test
    @DisplayName("次优先级健康渠道可被选：高优先级熔断后回退到健康次优先级")
    void secondaryPriorityHealthyChannel_selectedWhenHigherPriorityCircuitBroken() {
        // ch1：priority=1（最高优先级）但熔断中
        ModelInstance ch1 = new ModelInstance();
        ch1.setId(1L);
        ch1.setChannelId(100L);
        ch1.setPriority(1);
        // ch2：priority=2（次优先级）且健康
        ModelInstance ch2 = new ModelInstance();
        ch2.setId(2L);
        ch2.setChannelId(200L);
        ch2.setPriority(2);

        // 按入站协议 OPENAI 派生 endpointId：channel 100→endpoint 150（熔断）；channel 200→endpoint 250（健康）
        ChannelEndpoint ep1 = new ChannelEndpoint();
        ep1.setId(150L);
        ep1.setChannelId(100L);
        ep1.setProtocol(Protocol.OPENAI);
        ChannelEndpoint ep2 = new ChannelEndpoint();
        ep2.setId(250L);
        ep2.setChannelId(200L);
        ep2.setProtocol(Protocol.OPENAI);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep1);
        when(endpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(ep2);
        when(circuitBreakerManager.isAvailable(150L)).thenReturn(false);
        when(circuitBreakerManager.isAvailable(250L)).thenReturn(true);

        // 真实 Health + Priority + LoadBalanceRouter（Task 3.1 已降级为透传，返回候选列表）
        LoadBalanceRouter loadBalance = new LoadBalanceRouter();
        RouterChain chain = new RouterChain(List.of(
                new HealthRouter(circuitBreakerManager, endpointResolver),
                new PriorityRouter(),
                loadBalance));

        List<ModelInstance> result = chain.filter(List.of(ch1, ch2),
                new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI));

        // Health 先过滤掉熔断的 ch1 → [ch2]；Priority 在剩余 ch2 上选 → [ch2]；LoadBalance 透传 [ch2]
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("PriorityRouter 经路由链后保留全部 priority 组（不收敛）")
    void priorityRouter_keepsAllPriorityGroups_throughChain() {
        ModelInstance primary = new ModelInstance();
        primary.setId(1L);
        primary.setPriority(1);
        ModelInstance backup = new ModelInstance();
        backup.setId(2L);
        backup.setPriority(2);

        RouterChain chain = new RouterChain(List.of(new PriorityRouter()));
        RoutingRequest request = new RoutingRequest(1L, 1L, "USER", RoutingStrategy.WEIGHTED);

        List<ModelInstance> result = chain.filter(List.of(primary, backup), request);

        // 不收敛：经 PriorityRouter 后仍含全部 priority 组，按升序 [主,备]，供 L1 故障转移逐个尝试
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelInstance::getId).containsExactly(1L, 2L);
    }

    /** 读取 RouterChain 私有 routers 字段（已按 @Order 排序的责任链） */
    @SuppressWarnings("unchecked")
    private List<Router> readRouters(RouterChain chain) throws Exception {
        Field field = RouterChain.class.getDeclaredField("routers");
        field.setAccessible(true);
        return (List<Router>) field.get(chain);
    }
}
