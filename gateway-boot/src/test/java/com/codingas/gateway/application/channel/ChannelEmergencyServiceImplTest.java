package com.codingas.gateway.application.channel;

import com.codingas.gateway.common.exception.GatewayRequestException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
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

        @Test
        @DisplayName("端点 channelId 为脏数据 null 时抛业务异常而非 NPE")
        void forceOpen_endpointChannelIdNull_throwsBusinessException() {
            // 脏数据/旧数据场景：ChannelEndpoint.channelId 为 null
            ChannelEndpoint endpoint = buildEndpoint(10L, null);
            when(channelEndpointGateway.findById(10L)).thenReturn(Optional.of(endpoint));

            assertThatThrownBy(() -> channelEmergencyService.forceOpen(1L, 10L))
                    .isInstanceOf(GatewayRequestException.class)
                    .hasMessageContaining("不属于");
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
}
