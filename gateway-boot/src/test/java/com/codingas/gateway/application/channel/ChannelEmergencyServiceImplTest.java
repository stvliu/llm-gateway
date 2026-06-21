package com.codingas.gateway.application.channel;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.resilience.entity.Cluster;
import com.codingas.gateway.domain.resilience.gateway.ClusterGateway;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.infrastructure.resilience.ChannelEndpointCircuitBreakerManager;
import com.codingas.gateway.infrastructure.resilience.CircuitBreakerState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ChannelEmergencyServiceImpl 单元测试
 *
 * <p>Mock {@link ChannelEndpointCircuitBreakerManager} 与各 Gateway，
 * 验证应急操作的业务校验与委托逻辑。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("渠道应急操作服务测试")
class ChannelEmergencyServiceImplTest {

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    @Mock
    private ChannelEndpointGateway channelEndpointGateway;

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ClusterGateway clusterGateway;

    @InjectMocks
    private ChannelEmergencyServiceImpl channelEmergencyService;

    @Nested
    @DisplayName("forceOpen 应急熔断")
    class ForceOpenTests {

        @Test
        @DisplayName("端点属于渠道时强制熔断并返回 OPEN")
        void forceOpen_validEndpoint_returnsOpen() {
            stubEndpointBelongsToChannel(1L, 10L);
            when(circuitBreakerManager.getState(10L)).thenReturn(CircuitBreakerState.OPEN);

            String result = channelEmergencyService.forceOpen(1L, 10L);

            assertThat(result).isEqualTo("OPEN");
            verify(circuitBreakerManager).forceOpen(10L);
        }

        @Test
        @DisplayName("端点不存在时抛出异常")
        void forceOpen_endpointNotFound_throwsException() {
            when(channelEndpointGateway.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelEmergencyService.forceOpen(1L, 10L))
                    .isInstanceOf(GatewayRequestException.class);
            verify(circuitBreakerManager, never()).forceOpen(any());
        }

        @Test
        @DisplayName("端点不属于该渠道时抛出异常")
        void forceOpen_endpointNotBelongToChannel_throwsException() {
            ChannelEndpoint endpoint = buildEndpoint(10L, 999L); // 属于渠道 999
            when(channelEndpointGateway.findById(10L)).thenReturn(Optional.of(endpoint));

            assertThatThrownBy(() -> channelEmergencyService.forceOpen(1L, 10L))
                    .isInstanceOf(GatewayRequestException.class);
            verify(circuitBreakerManager, never()).forceOpen(any());
        }
    }

    @Nested
    @DisplayName("forceClose 应急恢复")
    class ForceCloseTests {

        @Test
        @DisplayName("端点属于渠道时强制恢复并返回 CLOSED")
        void forceClose_validEndpoint_returnsClosed() {
            stubEndpointBelongsToChannel(1L, 10L);
            when(circuitBreakerManager.getState(10L)).thenReturn(CircuitBreakerState.CLOSED);

            String result = channelEmergencyService.forceClose(1L, 10L);

            assertThat(result).isEqualTo("CLOSED");
            verify(circuitBreakerManager).forceClose(10L);
        }
    }

    @Nested
    @DisplayName("getState 状态查询")
    class GetStateTests {

        @Test
        @DisplayName("端点属于渠道时返回当前状态")
        void getState_validEndpoint_returnsState() {
            stubEndpointBelongsToChannel(1L, 10L);
            when(circuitBreakerManager.getState(10L)).thenReturn(CircuitBreakerState.HALF_OPEN);

            String result = channelEmergencyService.getState(1L, 10L);

            assertThat(result).isEqualTo("HALF_OPEN");
            verify(circuitBreakerManager, never()).forceOpen(any());
            verify(circuitBreakerManager, never()).forceClose(any());
        }
    }

    @Nested
    @DisplayName("switchCluster 紧切域")
    class SwitchClusterTests {

        @Test
        @DisplayName("渠道与目标域存在时切换 clusterId 并保存")
        void switchCluster_valid_returnsSuccess() {
            Channel channel = buildChannel(1L, 100L);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));
            Cluster target = buildCluster(200L, "claude-sg");
            when(clusterGateway.findById(200L)).thenReturn(target);

            channelEmergencyService.switchCluster(1L, 200L);

            assertThat(channel.getClusterId()).isEqualTo(200L);
            verify(channelGateway).save(channel);
        }

        @Test
        @DisplayName("渠道不存在时抛出异常")
        void switchCluster_channelNotFound_throwsException() {
            when(channelGateway.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> channelEmergencyService.switchCluster(999L, 200L))
                    .isInstanceOf(GatewayRequestException.class);
            verify(channelGateway, never()).save(any());
        }

        @Test
        @DisplayName("目标故障域不存在时抛出异常")
        void switchCluster_clusterNotFound_throwsException() {
            Channel channel = buildChannel(1L, 100L);
            when(channelGateway.findById(1L)).thenReturn(Optional.of(channel));
            when(clusterGateway.findById(200L)).thenReturn(null);

            assertThatThrownBy(() -> channelEmergencyService.switchCluster(1L, 200L))
                    .isInstanceOf(GatewayRequestException.class);
            verify(channelGateway, never()).save(any());
        }
    }

    // ===== Helper methods =====

    private void stubEndpointBelongsToChannel(Long channelId, Long endpointId) {
        ChannelEndpoint endpoint = buildEndpoint(endpointId, channelId);
        when(channelEndpointGateway.findById(endpointId)).thenReturn(Optional.of(endpoint));
    }

    private ChannelEndpoint buildEndpoint(Long id, Long channelId) {
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setId(id);
        endpoint.setChannelId(channelId);
        return endpoint;
    }

    private Channel buildChannel(Long id, Long clusterId) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setClusterId(clusterId);
        return channel;
    }

    private Cluster buildCluster(Long id, String code) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setCode(code);
        return cluster;
    }
}
