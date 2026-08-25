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
package com.codingas.gateway.proxy.routing;

import com.codingas.gateway.common.exception.ResourceNotFoundException;
import com.codingas.gateway.iam.application.Application;
import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.iam.application.ApplicationRepository;
import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.model.ModelInstance;
import com.codingas.gateway.provider.model.Model;
import com.codingas.gateway.provider.channel.ChannelState;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.upstream.RoutingStrategy;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.upstream.RoutingContext;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private InstanceSelector instanceSelector;


    @Mock
    private CredentialResolver credentialResolver;

    @Mock
    private EndpointResolver endpointResolver;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ApplicationRepository applicationRepository;

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

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(100L);
            modelInstance.setModelId(1L);
            modelInstance.setState(ModelInstance.State.ACTIVE);

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
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI)).thenReturn(List.of(modelInstance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-test-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));

            // when
            RoutingContext result = routingResolver.resolve("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

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
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI)).thenReturn(List.of(modelInstance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-ant-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));

            // when — 入站协议是 OPENAI，端点协议是 ANTHROPIC
            RoutingContext result = routingResolver.resolve("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

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
            assertThatThrownBy(() -> routingResolver.resolve("non-existent", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Model");
        }

        @Test
        @DisplayName("渠道不存在时抛出异常")
        void resolve_channelNotFound_throwsException() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance modelInstance = new ModelInstance();
            modelInstance.setId(10L);
            modelInstance.setChannelId(999L);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(999L);
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI)).thenReturn(List.of(modelInstance));
            when(credentialResolver.resolve(999L)).thenReturn("sk-key");
            when(endpointResolver.resolve(999L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> routingResolver.resolve("gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Channel");
        }
    }

    @Nested
    @DisplayName("resolveCandidates 候选列表解析")
    class ResolveCandidatesTests {

        @Test
        @DisplayName("多候选时返回按 priority 排序的多个 RoutingContext")
        void resolveCandidates_multipleInstances_returnsContextsByPriority() {
            // given — 两个候选实例（InstanceSelector 已按 priority 升序返回）
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            // 候选1：channel 100（priority 低，先返回）
            ModelInstance instance1 = new ModelInstance();
            instance1.setId(10L);
            instance1.setChannelId(100L);
            instance1.setModelId(1L);
            instance1.setUpstreamModelName("gpt-4o-upstream-1");

            // 候选2：channel 200（priority 高，后返回）
            ModelInstance instance2 = new ModelInstance();
            instance2.setId(20L);
            instance2.setChannelId(200L);
            instance2.setModelId(1L);
            instance2.setUpstreamModelName("gpt-4o-upstream-2");

            Channel channel1 = new Channel();
            channel1.setId(100L);
            channel1.setTimeout(30);

            Channel channel2 = new Channel();
            channel2.setId(200L);
            channel2.setTimeout(60);

            ChannelEndpoint endpoint1 = new ChannelEndpoint();
            endpoint1.setId(50L);
            endpoint1.setChannelId(100L);
            endpoint1.setEndpointUrl("https://api1.openai.com/v1");
            endpoint1.setProtocol(Protocol.OPENAI);

            ChannelEndpoint endpoint2 = new ChannelEndpoint();
            endpoint2.setId(60L);
            endpoint2.setChannelId(200L);
            endpoint2.setEndpointUrl("https://api2.openai.com/v1");
            endpoint2.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance1, instance2));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key-1");
            when(credentialResolver.resolve(200L)).thenReturn("sk-key-2");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint1);
            when(endpointResolver.resolve(200L, Protocol.OPENAI)).thenReturn(endpoint2);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel1));
            when(channelRepository.findById(200L)).thenReturn(Optional.of(channel2));

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 返回数量与候选数一致，顺序保持 InstanceSelector 的 priority 升序
            assertThat(results).hasSize(2);
            // 第一个候选对应 instance1（channel 100）
            assertThat(results.get(0).channelId()).isEqualTo(100L);
            assertThat(results.get(0).channelEndpointId()).isEqualTo(50L);
            assertThat(results.get(0).endpointUrl()).isEqualTo("https://api1.openai.com/v1");
            assertThat(results.get(0).upstreamProtocol()).isEqualTo(Protocol.OPENAI);
            assertThat(results.get(0).providerApiKey()).isEqualTo("sk-key-1");
            assertThat(results.get(0).timeout()).isEqualTo(30);
            assertThat(results.get(0).needsProtocolAdaptation()).isFalse();
            assertThat(results.get(0).modelName()).isEqualTo("gpt-4o");
            assertThat(results.get(0).upstreamModelName()).isEqualTo("gpt-4o-upstream-1");
            // 第二个候选对应 instance2（channel 200）
            assertThat(results.get(1).channelId()).isEqualTo(200L);
            assertThat(results.get(1).channelEndpointId()).isEqualTo(60L);
            assertThat(results.get(1).endpointUrl()).isEqualTo("https://api2.openai.com/v1");
            assertThat(results.get(1).providerApiKey()).isEqualTo("sk-key-2");
            assertThat(results.get(1).timeout()).isEqualTo(60);
            assertThat(results.get(1).upstreamModelName()).isEqualTo("gpt-4o-upstream-2");
        }

        @Test
        @DisplayName("单候选时返回单元素列表")
        void resolveCandidates_singleInstance_returnsSingleContext() {
            // given
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);
            instance.setUpstreamModelName("gpt-4o-upstream");

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setEndpointUrl("https://api.openai.com/v1");
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-test-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).channelId()).isEqualTo(100L);
            assertThat(results.get(0).providerApiKey()).isEqualTo("sk-test-key");
            assertThat(results.get(0).upstreamModelName()).isEqualTo("gpt-4o-upstream");
        }

        @Test
        @DisplayName("无可用实例时抛出 ResourceNotFoundException（由 InstanceSelector 透传）")
        void resolveCandidates_noInstance_throwsException() {
            // given — InstanceSelector 在无候选时抛 ResourceNotFoundException
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenThrow(new ResourceNotFoundException("ModelInstance", model.getId()));

            // when & then — resolveCandidates 应透传异常
            assertThatThrownBy(() -> routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("ModelInstance");
        }

        @Test
        @DisplayName("带 applicationId 的路由 — applicationId 透传至 InstanceSelector 且返回非空候选集（回归根因）")
        void resolveCandidates_withApplicationId_forwardsAndReturnsNonEmpty() {
            // given — 关键：applicationId=7L 必须被透传至 InstanceSelector，否则返回空集
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(1L, 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-test-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));

            // when
            List<RoutingContext> candidates = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 返回非空候选集（回归根因：applicationId 丢失时 InstanceSelector 返回空集）
            assertThat(candidates).isNotEmpty();
            assertThat(candidates.get(0).channelId()).isEqualTo(100L);
            // then — applicationId 被正确透传至 InstanceSelector
            verify(instanceSelector).select(1L, 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI);
        }
    }

    @Nested
    @DisplayName("Application.timeout 接入运行时")
    class ApplicationTimeoutTests {

        @Test
        @DisplayName("Application.timeout 非 0 时覆盖渠道默认超时")
        void resolveCandidates_applicationTimeoutNonZero_overridesChannelDefault() {
            // given — 渠道默认 30s，应用级 60s 应覆盖
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            Application app = new Application();
            app.setId(7L);
            app.setTimeout(60);
            app.setFailureStrategy(FailureStrategy.FAIL_OVER);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));
            lenient().when(applicationRepository.findById(7L)).thenReturn(app);

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 应用级 60 覆盖渠道默认 30
            assertThat(results.get(0).timeout()).isEqualTo(60);
            // then — 应用 failureStrategy 透传至 RoutingContext
            assertThat(results.get(0).failureStrategy()).isEqualTo(FailureStrategy.FAIL_OVER);
        }

        @Test
        @DisplayName("Application.timeout 为 0 时回退渠道默认超时")
        void resolveCandidates_applicationTimeoutZero_fallsBackToChannelDefault() {
            // given — 应用级 0 表示用渠道默认
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            Application app = new Application();
            app.setId(7L);
            app.setTimeout(0);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));
            lenient().when(applicationRepository.findById(7L)).thenReturn(app);

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 0 回退渠道默认 30
            assertThat(results.get(0).timeout()).isEqualTo(30);
        }

        @Test
        @DisplayName("applicationId 为 null 时不查 ApplicationRepository，用渠道默认超时")
        void resolveCandidates_applicationIdNull_usesChannelDefault() {
            // given — 无应用锚点，应直接用渠道默认
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), null, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, null, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 用渠道默认 30，且未查 ApplicationRepository
            assertThat(results.get(0).timeout()).isEqualTo(30);
            // then — 无应用锚点时 failureStrategy 默认 FAIL_RETRY
            assertThat(results.get(0).failureStrategy()).isEqualTo(FailureStrategy.FAIL_RETRY);
            verifyNoInteractions(applicationRepository);
        }

        @Test
        @DisplayName("Application 查不到时回退渠道默认超时，不抛异常")
        void resolveCandidates_applicationNotFound_fallsBackToChannelDefault() {
            // given — Application 查不到（findById 返回 null）
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));
            lenient().when(applicationRepository.findById(7L)).thenReturn(null);

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — 回退渠道默认 30，不抛异常
            assertThat(results.get(0).timeout()).isEqualTo(30);
            // then — Application 查不到时 failureStrategy 默认 FAIL_RETRY
            assertThat(results.get(0).failureStrategy()).isEqualTo(FailureStrategy.FAIL_RETRY);
        }
    }

    @Nested
    @DisplayName("Application.failureStrategy 透传至 RoutingContext")
    class ApplicationFailureStrategyTests {

        @Test
        @DisplayName("Application.failureStrategy 非 null 时透传至 RoutingContext")
        void resolveCandidates_applicationFailureStrategy_透传至RoutingContext() {
            // given — 应用策略 FAIL_FAST 应透传至候选
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            Application app = new Application();
            app.setId(7L);
            app.setTimeout(0);
            app.setFailureStrategy(FailureStrategy.FAIL_FAST);

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));
            lenient().when(applicationRepository.findById(7L)).thenReturn(app);

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — FAIL_FAST 透传
            assertThat(results.get(0).failureStrategy()).isEqualTo(FailureStrategy.FAIL_FAST);
        }

        @Test
        @DisplayName("Application.failureStrategy 为 null 时回退默认 FAIL_RETRY")
        void resolveCandidates_applicationFailureStrategyNull_默认FAIL_RETRY() {
            // given — 应用策略为 null（未配置），应回退默认 FAIL_RETRY
            Model model = new Model();
            model.setId(1L);
            model.setModelName("gpt-4o");

            ModelInstance instance = new ModelInstance();
            instance.setId(10L);
            instance.setChannelId(100L);
            instance.setModelId(1L);

            Channel channel = new Channel();
            channel.setId(100L);
            channel.setTimeout(30);

            ChannelEndpoint endpoint = new ChannelEndpoint();
            endpoint.setId(50L);
            endpoint.setChannelId(100L);
            endpoint.setProtocol(Protocol.OPENAI);

            Application app = new Application();
            app.setId(7L);
            app.setTimeout(0);
            // failureStrategy 显式置 null

            when(modelMatcher.match("gpt-4o")).thenReturn(model);
            when(instanceSelector.select(model.getId(), 7L, 1L, "USER", RoutingStrategy.WEIGHTED, Protocol.OPENAI))
                    .thenReturn(List.of(instance));
            when(credentialResolver.resolve(100L)).thenReturn("sk-key");
            when(endpointResolver.resolve(100L, Protocol.OPENAI)).thenReturn(endpoint);
            when(channelRepository.findById(100L)).thenReturn(Optional.of(channel));
            lenient().when(applicationRepository.findById(7L)).thenReturn(app);

            // when
            List<RoutingContext> results = routingResolver.resolveCandidates(
                    "gpt-4o", Protocol.OPENAI, 7L, 1L, "USER", RoutingStrategy.WEIGHTED);

            // then — null 回退默认 FAIL_RETRY
            assertThat(results.get(0).failureStrategy()).isEqualTo(FailureStrategy.FAIL_RETRY);
        }
    }
}
