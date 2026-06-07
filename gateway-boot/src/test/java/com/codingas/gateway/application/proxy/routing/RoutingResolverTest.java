package com.codingas.gateway.application.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.entity.ModelInstance;
import com.codingas.gateway.domain.supply.entity.Model;
import com.codingas.gateway.domain.supply.enums.*;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.domain.supply.valueobject.RoutingContext;
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
 * RoutingResolver 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoutingResolver 单元测试")
class RoutingResolverTest {

    @Mock
    private ModelMatcher modelMatcher;

    @Mock
    private ChannelSelector channelSelector;

    @Mock
    private CredentialResolver credentialResolver;

    @Mock
    private EndpointResolver endpointResolver;

    @Mock
    private ChannelGateway channelGateway;

    @InjectMocks
    private RoutingResolver routingResolver;

    @Nested
    @DisplayName("resolve 完整路由解析")
    class ResolveTests {

        @Test
        @DisplayName("完整解析成功 — 组装 RoutingContext")
        void resolve_allSteps_succeeds() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");
            model.setState(ModelState.ACTIVE);

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(100L);
            modelInstance.setModelId(1L);
            modelInstance.setState(ChannelModelState.ACTIVE);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setName("openai-main");
            channel.setState(ChannelState.ACTIVE);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setEndpointUrl("https://api.openai.com/v1");
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(channelSelector.select(1L, 1L)).thenReturn(modelInstance);
            when(credentialResolver.resolve(100L)).thenReturn("sk-test-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelGateway.findById(100L)).thenReturn(Optional.of(channel));

            // when
            RoutingContext result = routingResolver.resolve("gpt-4o", Protocol.OPENAI, 1L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.channelId()).isEqualTo(100L);
            assertThat(result.channelEndpointId()).isEqualTo(50L);
            assertThat(result.endpointUrl()).isEqualTo("https://api.openai.com/v1");
            assertThat(result.upstreamProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(result.providerApiKey()).isEqualTo("sk-test-key");
            assertThat(result.timeout()).isEqualTo(30);
            assertThat(result.needsProtocolAdaptation()).isFalse();
        }

        @Test
        @DisplayName("协议不同时 needsProtocolAdaptation 为 true")
        void resolve_differentProtocol_needsAdaptation() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");
            model.setState(ModelState.ACTIVE);

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(100L);
            modelInstance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setName("anthropic-via-openai");
            channel.setState(ChannelState.ACTIVE);
            channel.setTimeout(60);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setEndpointUrl("https://api.anthropic.com/v1");
            endpoint.setProtocol(Protocol.ANTHROPIC);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(channelSelector.select(1L, 1L)).thenReturn(modelInstance);
            when(credentialResolver.resolve(100L)).thenReturn("sk-ant-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelGateway.findById(100L)).thenReturn(Optional.of(channel));

            // when — 入站协议是 OPENAI，端点协议是 ANTHROPIC
            RoutingContext result = routingResolver.resolve("gpt-4o", Protocol.OPENAI, 1L);

            // then
            assertThat(result.needsProtocolAdaptation()).isTrue();
            assertThat(result.upstreamProtocol()).isEqualTo(Protocol.ANTHROPIC);
        }

        @Test
        @DisplayName("模型不存在时抛出异常")
        void resolve_modelNotFound_throwsException() {
            // given
            when(modelMatcher.match("non-existent"))
                    .thenThrow(new ResourceNotFoundException("Model", "non-existent"));

            // when & then
            assertThatThrownBy(() -> routingResolver.resolve("non-existent", Protocol.OPENAI, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Model");
        }

        @Test
        @DisplayName("通道不存在时抛出异常")
        void resolve_channelNotFound_throwsException() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");
            model.setState(ModelState.ACTIVE);

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(999L);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(999L);
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(channelSelector.select(1L, 1L)).thenReturn(modelInstance);
            when(credentialResolver.resolve(999L)).thenReturn("sk-key");
            when(endpointResolver.resolve(999L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelGateway.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> routingResolver.resolve("gpt-4o", Protocol.OPENAI, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Channel");
        }
    }
}
