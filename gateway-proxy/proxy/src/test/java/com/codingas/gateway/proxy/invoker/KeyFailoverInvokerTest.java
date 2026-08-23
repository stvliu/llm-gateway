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
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.common.enums.ProviderErrorType;
import com.codingas.gateway.protocol.transport.ProviderException;
import com.codingas.gateway.protocol.transport.ResilientClientFactory;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import com.codingas.gateway.provider.upstream.RoutingContext;
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

        when(resilientClient1.chat(request)).thenThrow(new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "fail"));
        ProtocolResponse expectedResponse = mock(ProtocolResponse.class);
        when(resilientClient2.chat(request)).thenReturn(expectedResponse);

        ProtocolResponse result = invoker.invoke(ctx, request);

        assertThat(result).isSameAs(expectedResponse);
        verify(resilientClient1).chat(request);
        verify(resilientClient2).chat(request);
    }

    @Test
    @DisplayName("所有 Key 失败时抛 ProviderException")
    void allKeysFailed_throwsProviderException() {
        ChannelCredential cred = new ChannelCredential();
        cred.setId(100L);
        cred.setApiKeyPlain("sk-key-1");

        when(credentialResolver.resolveAll(10L)).thenReturn(List.of(cred));
        when(circuitBreakerManager.isAvailable(20L)).thenReturn(true);

        UpstreamClient client = mock(UpstreamClient.class);
        when(clientRegistry.getClient("openai", "https://api.openai.com/v1", "sk-key-1", 60)).thenReturn(client);

        UpstreamClient resilientClient = mock(UpstreamClient.class);
        when(resilientClientFactory.wrap(client, 20L)).thenReturn(resilientClient);
        when(resilientClient.chat(request)).thenThrow(new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "fail"));

        assertThatThrownBy(() -> invoker.invoke(ctx, request))
                .isInstanceOf(ProviderException.class)
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
                .isInstanceOf(ProviderException.class);

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

        doThrow(new ProviderException(ProviderErrorType.UPSTREAM_ERROR, "启动失败"))
                .when(resilientClient1).chatStream(any(), any());

        StreamCallback callback = mock(StreamCallback.class);

        invoker.invokeStream(ctx, request, callback);

        verify(resilientClient1).chatStream(any(), any());
        verify(resilientClient2).chatStream(any(), any());
    }
}
