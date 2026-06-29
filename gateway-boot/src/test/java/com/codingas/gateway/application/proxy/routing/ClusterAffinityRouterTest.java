package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.domain.resilience.entity.ClusterHealthStatus;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.enums.RoutingStrategy;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ClusterAffinityRouter 单元测试
 *
 * <p>验证按域锁定语义：域内全部端点熔断（Cluster DOWN）→ 过滤整域实例；
 * DEGRADED/HEALTHY 域实例保留。Router 排在 HealthRouter(200) 之后、PriorityRouter(300) 之前。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClusterAffinityRouter 单元测试")
class ClusterAffinityRouterTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ClusterHealthAggregator clusterHealthAggregator;

    @Mock
    private EndpointResolver endpointResolver;

    @InjectMocks
    private ClusterAffinityRouter router;

    @Test
    @DisplayName("DOWN 域实例被过滤，HEALTHY 域实例保留")
    void downClusterFiltered_healthyKept() {
        // clusterA(10) 的实例 mi1：channel 100 → cluster 10
        // clusterB(20) 的实例 mi2：channel 200 → cluster 20
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 200L);

        when(channelGateway.findByIds(List.of(100L, 200L)))
                .thenReturn(List.of(channel(100L, 10L), channel(200L, 20L)));

        ChannelEndpoint ep1 = endpoint(500L, 100L);
        ChannelEndpoint ep2 = endpoint(600L, 200L);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep1);
        when(endpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(ep2);

        // clusterA 域全熔断 → DOWN；clusterB 健康
        when(clusterHealthAggregator.aggregate(List.of(500L))).thenReturn(ClusterHealthStatus.DOWN);
        when(clusterHealthAggregator.aggregate(List.of(600L))).thenReturn(ClusterHealthStatus.HEALTHY);

        RoutingRequest request = request(Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("DEGRADED 域实例保留（容量受损但仍可用）")
    void degradedCluster_kept() {
        ModelInstance mi = instance(1L, 100L);
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel(100L, 10L)));
        ChannelEndpoint ep = endpoint(500L, 100L);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep);
        when(clusterHealthAggregator.aggregate(List.of(500L))).thenReturn(ClusterHealthStatus.DEGRADED);

        RoutingRequest request = request(Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("同域多实例：域 DOWN 时整域实例全过滤")
    void sameClusterMultipleInstances_allFilteredWhenDown() {
        // 同 cluster 10 下两个实例（channel 100、101）
        ModelInstance mi1 = instance(1L, 100L);
        ModelInstance mi2 = instance(2L, 101L);
        when(channelGateway.findByIds(List.of(100L, 101L)))
                .thenReturn(List.of(channel(100L, 10L), channel(101L, 10L)));
        ChannelEndpoint ep1 = endpoint(500L, 100L);
        ChannelEndpoint ep2 = endpoint(501L, 101L);
        when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(ep1);
        when(endpointResolver.resolve(101L, Protocol.OPENAI)).thenReturn(ep2);
        // 同域两 endpoint 聚合 → DOWN
        when(clusterHealthAggregator.aggregate(List.of(500L, 501L))).thenReturn(ClusterHealthStatus.DOWN);

        RoutingRequest request = request(Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi1, mi2), request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("空候选列表直接返回空，不查 Channel/熔断")
    void emptyInput_returnsEmpty() {
        RoutingRequest request = request(Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(), request);

        assertThat(result).isEmpty();
        verify(channelGateway, never()).findByIds(anyList());
    }

    @Test
    @DisplayName("未关联 cluster 的实例（clusterId null）保守保留，不参与域聚合")
    void noClusterId_kept() {
        // channel 100 未分配 cluster（clusterId=null）
        ModelInstance mi = instance(1L, 100L);
        when(channelGateway.findByIds(List.of(100L))).thenReturn(List.of(channel(100L, null)));

        RoutingRequest request = request(Protocol.OPENAI);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("protocol 为 null 时保守保留实例（无法派生 endpointId 判断域健康）")
    void protocolNull_kept() {
        ModelInstance mi = instance(1L, 100L);
        // 不 mock channelGateway，因 protocol null 应短路

        RoutingRequest request = new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, null);
        List<ModelInstance> result = router.filter(List.of(mi), request);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("isForce 返回 false（DOWN 过滤后空时让链继续，不终止）")
    void isForce_returnsFalse() {
        assertThat(router.isForce()).isFalse();
    }

    /** 构造测试用 ModelInstance */
    private ModelInstance instance(long id, long channelId) {
        ModelInstance mi = new ModelInstance();
        mi.setId(id);
        mi.setChannelId(channelId);
        return mi;
    }

    /** 构造测试用 Channel */
    private Channel channel(long id, Long clusterId) {
        Channel ch = new Channel();
        ch.setId(id);
        ch.setClusterId(clusterId);
        return ch;
    }

    /** 构造测试用 ChannelEndpoint */
    private ChannelEndpoint endpoint(long id, long channelId) {
        ChannelEndpoint ep = new ChannelEndpoint();
        ep.setId(id);
        ep.setChannelId(channelId);
        return ep;
    }

    /** 构造 OPENAI 协议的路由请求 */
    private RoutingRequest request(Protocol protocol) {
        return new RoutingRequest(1L, 1L, 1L, "USER", RoutingStrategy.WEIGHTED, protocol);
    }
}
