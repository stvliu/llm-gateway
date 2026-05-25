package com.codingas.gateway.domain.supply.service;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelDomainService 测试")
class ChannelDomainServiceTest {

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ChannelEndpointGateway channelEndpointGateway;

    private ChannelDomainService service;

    @BeforeEach
    void setUp() {
        service = new ChannelDomainService(channelGateway, channelEndpointGateway);
    }

    @Nested
    @DisplayName("resolveEndpoint 测试")
    class ResolveEndpointTests {

        @Test
        @DisplayName("优先匹配同名协议端点")
        void resolveEndpoint_matchSameProtocol() {
            Channel channel = new Channel();
            channel.setId(1L);

            ChannelEndpoint anthropicEndpoint = new ChannelEndpoint();
            anthropicEndpoint.setId(10L);
            anthropicEndpoint.setProtocol(Protocol.ANTHROPIC);
            anthropicEndpoint.setEndpointUrl("https://api.anthropic.com");
            anthropicEndpoint.setState(ChannelEndpointState.ACTIVE);

            ChannelEndpoint openaiEndpoint = new ChannelEndpoint();
            openaiEndpoint.setId(11L);
            openaiEndpoint.setProtocol(Protocol.OPENAI);
            openaiEndpoint.setEndpointUrl("https://api.openai.com");
            openaiEndpoint.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of(openaiEndpoint, anthropicEndpoint));

            ChannelEndpoint result = service.resolveEndpoint(channel, Protocol.ANTHROPIC);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getProtocol()).isEqualTo(Protocol.ANTHROPIC);
        }

        @Test
        @DisplayName("无匹配端点时降级选第一个可用端点")
        void resolveEndpoint_fallbackToFirstAvailable() {
            Channel channel = new Channel();
            channel.setId(1L);

            ChannelEndpoint anthropicEndpoint = new ChannelEndpoint();
            anthropicEndpoint.setId(10L);
            anthropicEndpoint.setProtocol(Protocol.ANTHROPIC);
            anthropicEndpoint.setEndpointUrl("https://api.anthropic.com");
            anthropicEndpoint.setState(ChannelEndpointState.ACTIVE);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of(anthropicEndpoint));

            ChannelEndpoint result = service.resolveEndpoint(channel, Protocol.OPENAI);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getProtocol()).isEqualTo(Protocol.ANTHROPIC);
        }

        @Test
        @DisplayName("渠道无可用端点时抛出异常")
        void resolveEndpoint_noAvailableEndpoint_throwsException() {
            Channel channel = new Channel();
            channel.setId(1L);

            when(channelEndpointGateway.findActiveByChannelId(1L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.resolveEndpoint(channel, Protocol.OPENAI))
                    .isInstanceOf(com.codingas.gateway.domain.supply.exception.ChannelException.class)
                    .satisfies(ex -> assertThat(((com.codingas.gateway.domain.supply.exception.ChannelException) ex).getCode()).isEqualTo("CHANNEL_NO_ENDPOINT"))
                    .hasMessageContaining("渠道无可用端点");
        }
    }
}
