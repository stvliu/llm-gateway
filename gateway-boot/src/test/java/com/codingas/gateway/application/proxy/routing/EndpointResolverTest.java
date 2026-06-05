package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
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
import java.util.Optional;

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
        @DisplayName("解析成功 — 优先匹配协议同源端点")
        void resolve_protocolMatched_returnsEndpoint() {
            // given
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(1L);
            endpoint.setChannelId(10L);
            endpoint.setEndpointUrl("https://api.openai.com/v1");
            endpoint.setProtocol(Protocol.OPENAI);

            when(channelEndpointGateway.findByChannelIdAndProtocol(10L, Protocol.OPENAI))
                    .thenReturn(Optional.of(endpoint));

            // when
            ChannelEndpoint result = endpointResolver.resolve(10L, Protocol.OPENAI);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEndpointUrl()).isEqualTo("https://api.openai.com/v1");
            assertThat(result.getProtocol()).isEqualTo(Protocol.OPENAI);
        }

        @Test
        @DisplayName("无协议匹配端点时，回退到任意端点")
        void resolve_noProtocolMatch_fallsBackToAny() {
            // given
            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(1L);
            endpoint.setChannelId(10L);
            endpoint.setEndpointUrl("https://api.anthropic.com/v1");
            endpoint.setProtocol(Protocol.ANTHROPIC);

            when(channelEndpointGateway.findByChannelIdAndProtocol(10L, Protocol.OPENAI))
                    .thenReturn(Optional.empty());
            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of(endpoint));

            // when
            ChannelEndpoint result = endpointResolver.resolve(10L, Protocol.OPENAI);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getEndpointUrl()).isEqualTo("https://api.anthropic.com/v1");
        }

        @Test
        @DisplayName("无端点记录时抛出 ResourceNotFoundException")
        void resolve_emptyList_throwsException() {
            // given
            when(channelEndpointGateway.findByChannelIdAndProtocol(10L, Protocol.OPENAI))
                    .thenReturn(Optional.empty());
            when(channelEndpointGateway.findByChannelId(10L))
                    .thenReturn(List.of());

            // when & then
            assertThatThrownBy(() -> endpointResolver.resolve(10L, Protocol.OPENAI))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ChannelEndpoint")
                    .hasMessageContaining("10");
        }
    }
}