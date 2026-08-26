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
package com.codingas.gateway.proxy.invoker;

import com.codingas.gateway.proxy.routing.CredentialResolver;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.ProtocolResponse;
import com.codingas.gateway.protocol.StreamCallback;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.UpstreamException;
import com.codingas.gateway.protocol.transport.ResilientClientFactory;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import com.codingas.gateway.proxy.routing.RoutingContext;
import com.codingas.gateway.common.enums.FailureStrategy;
import com.codingas.gateway.resilience.circuitbreaker.ChannelEndpointCircuitBreakerManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KeyFailoverInvoker 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KeyFailoverInvoker 单元测试")
class KeyFailoverInvokerTest {

    @Mock
    private CredentialResolver credentialResolver;

    @Mock
    private UpstreamClientRegistry clientRegistry;

    @Mock
    private ResilientClientFactory resilientClientFactory;

    @Mock
    private ChannelEndpointCircuitBreakerManager circuitBreakerManager;

    private MeterRegistry meterRegistry;
    private KeyFailoverInvoker invoker;

    private RoutingContext ctx;
    private ProtocolRequest request;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        invoker = new KeyFailoverInvoker(credentialResolver, clientRegistry,
                resilientClientFactory, circuitBreakerManager, meterRegistry);

        ctx = new RoutingContext(10L, 20L, "https://api.openai.com/v1",
                Protocol.OPENAI, "sk-test", 60, false, "test-model", null,
                FailureStrategy.FAIL_RETRY);

        request = mock(ProtocolRequest.class);
        lenient().when(request.getModel()).thenReturn("gpt-4o");
    }

    @Test
    @DisplayName("第一个 Key 成功时直接返回")
    void firstKeySuccess_returns() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);

        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);

        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(resilientClient.chat(request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx, request);

        assertThat(result).isSameAs(expectedResponse);
        verify(credentialResolver).resolveAll(10L);
    }

    @Test
    @DisplayName("第一个 Key 失败时自动切换到下一个")
    void failover_toNextKey() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        cred1.setApiKeyPlain("sk-key-1");
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);
        cred2.setApiKeyPlain("sk-key-2");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client1 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client1);
        UpstreamClient client2 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-2", 60)).thenReturn(client2);

        UpstreamClient resilientClient1 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client1, 20L)).thenReturn(resilientClient1);
        UpstreamClient resilientClient2 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client2, 20L)).thenReturn(resilientClient2);

        when(resilientClient1.chat(request)).thenThrow(new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "fail"));
        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(resilientClient2.chat(request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx, request);

        assertThat(result).isSameAs(expectedResponse);
        verify(resilientClient1).chat(request);
        verify(resilientClient2).chat(request);
    }

    @Test
    @DisplayName("所有 Key 失败时抛 UpstreamException")
    void allKeysFailed_throwsUpstreamException() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);

        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);
        when(resilientClient.chat(request)).thenThrow(new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "fail"));

        assertThatThrownBy(() -> invoker.invoke(ctx, request))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("所有 Key 均失败");
    }

    @Test
    @DisplayName("熔断中的端点被跳过")
    void circuitBreakerOpen_skipped() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(false);

        assertThatThrownBy(() -> invoker.invoke(ctx, request))
                .isInstanceOf(UpstreamException.class);

        verify(clientRegistry, never()).getClient(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("流式调用启动失败时切换到下一个 Key")
    void invokeStream_failoverToNextKey() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        cred1.setApiKeyPlain("sk-key-1");
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);
        cred2.setApiKeyPlain("sk-key-2");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client1 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client1);
        UpstreamClient client2 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-2", 60)).thenReturn(client2);

        UpstreamClient resilientClient1 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client1, 20L)).thenReturn(resilientClient1);
        UpstreamClient resilientClient2 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client2, 20L)).thenReturn(resilientClient2);

        doThrow(new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "启动失败"))
                .when(resilientClient1).chatStream(any(), any());

        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx, request, callback);

        verify(resilientClient1).chatStream(any(), any());
        verify(resilientClient2).chatStream(any(), any());
    }

    @Test
    @DisplayName("非流式调用无可用 Key 时抛异常（无 Key 可试）")
    void invoke_noCredentials_throws() {
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> invoker.invoke(ctx, request))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("所有 Key 均失败")
                .hasMessageContaining("无可用 Key");
    }

    @Test
    @DisplayName("所有 Key 均处于熔断跳过时抛异常并计数 exhausted")
    void invoke_allKeysCircuitOpen_exhausted() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(false);

        assertThatThrownBy(() -> invoker.invoke(ctx, request))
                .isInstanceOf(UpstreamException.class);
        verify(clientRegistry, never()).getClient(anyString(), anyString(), anyString(), anyInt());
        assertThat(meterRegistry.counter("gateway.failover.exhausted",
                "provider", "openai", "channel_id", "10").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("invokeSingleKey 无可用 Key 时抛异常")
    void invokeSingleKey_noCredentials_throws() {
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> invoker.invokeSingleKey(ctx, request))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("无可用 Key");
    }

    @Test
    @DisplayName("invokeSingleKey 端点熔断中直接抛异常，不试 Key")
    void invokeSingleKey_circuitOpen_throws() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(false);

        assertThatThrownBy(() -> invoker.invokeSingleKey(ctx, request))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("端点熔断中");
        verify(clientRegistry, never()).getClient(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("invokeSingleKey 首个 Key 成功直接返回")
    void invokeSingleKey_firstKeySuccess_returns() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);
        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);

        ProtocolResponse expected = mock(ProtocolResponse.class);
        when(resilientClient.chat(request)).thenReturn(expected);

        assertThat(invoker.invokeSingleKey(ctx, request)).isSameAs(expected);
    }

    @Test
    @DisplayName("invokeSingleKey 首个 Key 失败直接抛出，不换 Key")
    void invokeSingleKey_firstKeyFailed_throws() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);
        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);

        UpstreamException failure = new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "失败");
        when(resilientClient.chat(request)).thenThrow(failure);

        assertThatThrownBy(() -> invoker.invokeSingleKey(ctx, request)).isSameAs(failure);
        assertThat(meterRegistry.counter("gateway.failover.triggered",
                "provider", "openai", "from_key", "100", "error_type", "UPSTREAM_ERROR").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("invokeSingleKeyStream 无可用 Key 时抛异常")
    void invokeSingleKeyStream_noCredentials_throws() {
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> invoker.invokeSingleKeyStream(ctx, request, mock(StreamCallback.class)))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("无可用 Key");
    }

    @Test
    @DisplayName("invokeSingleKeyStream 端点熔断中直接抛异常")
    void invokeSingleKeyStream_circuitOpen_throws() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(false);

        assertThatThrownBy(() -> invoker.invokeSingleKeyStream(ctx, request, mock(StreamCallback.class)))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("端点熔断中");
    }

    @Test
    @DisplayName("invokeSingleKeyStream 首个 Key 流式启动成功")
    void invokeSingleKeyStream_firstKeySuccess() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);
        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);

        StreamCallback callback = mock(StreamCallback.class);
        invoker.invokeSingleKeyStream(ctx, request, callback);

        verify(resilientClient).chatStream(request, callback);
    }

    @Test
    @DisplayName("invokeSingleKeyStream 首个 Key 流式启动失败直接抛出")
    void invokeSingleKeyStream_firstKeyFailed_throws() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");
        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);
        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);

        UpstreamException failure = new UpstreamException(ProviderErrorType.RATE_LIMIT_ERROR, "限流");
        doThrow(failure).when(resilientClient).chatStream(any(), any());

        assertThatThrownBy(() -> invoker.invokeSingleKeyStream(ctx, request, mock(StreamCallback.class)))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("流式调用首个 Key 成功即返回，不试后续 Key")
    void invokeStream_firstKeySuccess_returns() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        cred1.setApiKeyPlain("sk-key-1");
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);
        cred2.setApiKeyPlain("sk-key-2");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        // 仅 stub 第一个 Key：成功后立即返回，第二个 Key 不会被获取
        UpstreamClient client1 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client1);
        UpstreamClient resilientClient1 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client1, 20L)).thenReturn(resilientClient1);

        StreamCallback callback = mock(StreamCallback.class);
        invoker.invokeStream(ctx, request, callback);

        verify(resilientClient1).chatStream(request, callback);
        verify(clientRegistry, never()).getClient(eq("openai"), anyString(), eq("sk-key-2"), anyInt());
    }

    @Test
    @DisplayName("流式调用所有 Key 启动失败时抛异常并计数 exhausted")
    void invokeStream_allKeysFailed_throws() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        cred1.setApiKeyPlain("sk-key-1");
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);
        cred2.setApiKeyPlain("sk-key-2");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client1 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client1);
        UpstreamClient client2 = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-2", 60)).thenReturn(client2);

        UpstreamClient resilientClient1 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client1, 20L)).thenReturn(resilientClient1);
        UpstreamClient resilientClient2 = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client2, 20L)).thenReturn(resilientClient2);

        doThrow(new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "启动失败"))
                .when(resilientClient1).chatStream(any(), any());
        doThrow(new UpstreamException(ProviderErrorType.UPSTREAM_ERROR, "启动失败"))
                .when(resilientClient2).chatStream(any(), any());

        assertThatThrownBy(() -> invoker.invokeStream(ctx, request, mock(StreamCallback.class)))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("流式调用：所有 Key 均失败");
        assertThat(meterRegistry.counter("gateway.failover.exhausted",
                "provider", "openai", "channel_id", "10").count()).isEqualTo(1);
    }

    @Test
    @DisplayName("流式调用所有 Key 处于熔断时抛异常（无 Key 尝试）")
    void invokeStream_allKeysCircuitOpen_throws() {
        ChannelCredential cred1 = new ChannelCredential();
        cred1.setId(100L);
        ChannelCredential cred2 = new ChannelCredential();
        cred2.setId(200L);

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred1, cred2));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(false);

        assertThatThrownBy(() -> invoker.invokeStream(ctx, request, mock(StreamCallback.class)))
                .isInstanceOf(UpstreamException.class)
                .hasMessageContaining("流式调用：所有 Key 均失败");
        verify(clientRegistry, never()).getClient(anyString(), anyString(), anyString(), anyInt());
    }
}
