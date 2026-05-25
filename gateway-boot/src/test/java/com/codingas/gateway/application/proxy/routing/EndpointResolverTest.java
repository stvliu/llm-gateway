package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * EndpointResolver 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EndpointResolver 单元测试")
class EndpointResolverTest {

    @Mock
    private ChannelEndpointGateway channelEndpointGateway;

    @InjectMocks
    private EndpointResolver endpointResolver;

    @Nested
    @DisplayName("resolve 端点解析")
    class ResolveTests {

        @Test
        @DisplayName("解析成功 — 返回可用端点")
        void resolve_activeEndpoint_returnsEndpoint() {
            // given
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(1L);
            endpoint.setChannelId(10L);
            endpoint.setEndpointUrl("https://api.openai.com/v1");
            endpoint.setProtocol(Protocol.OPENAI);
            endpoint.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of(endpoint));

            // when
            ChannelEndpoint result = endpointResolver.resolve(10L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEndpointUrl()).isEqualTo("https://api.openai.com/v1");
            assertThat(result.getProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(result.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("跳过已禁用端点，返回可用端点")
        void resolve_disabledEndpointSkipped_returnsActive() {
            // given
            ChannelEndpoint disabled = new ChannelEndpoint();
            disabled.setId(1L);
            disabled.setChannelId(10L);
            disabled.setEndpointUrl("https://disabled.example.com");
            disabled.setProtocol(Protocol.OPENAI);
            disabled.setState(ChannelEndpointState.DISABLED);

            ChannelEndpoint active = new ChannelEndpoint();
            active.setId(2L);
            active.setChannelId(10L);
            active.setEndpointUrl("https://api.openai.com/v1");
            active.setProtocol(Protocol.OPENAI);
            active.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of(disabled, active));

            // when
            ChannelEndpoint result = endpointResolver.resolve(10L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getEndpointUrl()).isEqualTo("https://api.openai.com/v1");
        }

        @Test
        @DisplayName("无可用端点时抛出 ResourceNotFoundException")
        void resolve_noEndpoint_throwsException() {
            // given
            ChannelEndpoint disabled = new ChannelEndpoint();
            disabled.setId(1L);
            disabled.setChannelId(10L);
            disabled.setState(ChannelEndpointState.DISABLED);

            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of(disabled));

            // when & then
            assertThatThrownBy(() -> endpointResolver.resolve(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelEndpoint")
                    .hasMessageContaining("10");
        }

        @Test
        @DisplayName("无端点记录时抛出 ResourceNotFoundException")
        void resolve_emptyList_throwsException() {
            // given
            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> endpointResolver.resolve(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelEndpoint")
                    .hasMessageContaining("10");
        }
    }
}
