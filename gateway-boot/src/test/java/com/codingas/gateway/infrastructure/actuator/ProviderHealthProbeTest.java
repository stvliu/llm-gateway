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
package com.codingas.gateway.infrastructure.actuator;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.channel.ChannelCredentialGateway;
import com.codingas.gateway.provider.channel.ChannelEndpointGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Provider 健康周期探活单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProviderHealthProbe")
class ProviderHealthProbeTest {

    @Mock
    private ChannelGateway channelGateway;
    @Mock
    private ChannelEndpointGateway endpointGateway;
    @Mock
    private ChannelCredentialGateway credentialGateway;
    @Mock
    private UpstreamClientRegistry clientRegistry;
    @Mock
    private ProviderHealthTracker healthTracker;
    @Mock
    private UpstreamClient upstreamClient;

    private ProviderHealthProbe probe() {
        return new ProviderHealthProbe(channelGateway, endpointGateway,
                credentialGateway, clientRegistry, healthTracker);
    }

    @Test
    @DisplayName("探活应遍历启用通道并更新供应商健康状态")
    void probe_probesActiveChannels() {
        Channel channel = new Channel();
        channel.setId(1L);
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(1L);
        endpoint.setProtocol(Protocol.OPENAI);
        endpoint.setEndpointUrl("https://api.openai.com/v1");
        ChannelCredential cred = new ChannelCredential();
        cred.setApiKeyPlain("sk-test");

        when(channelGateway.findAllActive()).thenReturn(List.of(channel));
        when(endpointGateway.findByChannelId(1L)).thenReturn(List.of(endpoint));
        when(credentialGateway.findActiveByChannelId(1L)).thenReturn(List.of(cred));
        when(clientRegistry.getClient(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(upstreamClient);
        when(upstreamClient.testConnectivity())
                .thenReturn(ConnectivityTestResult.success(1L, 42));

        probe().probe();

        verify(healthTracker).recordRequestResult("openai", true, null);
    }

    @Test
    @DisplayName("探活异常应记录为失败")
    void probe_exceptionRecordsFailure() {
        Channel channel = new Channel();
        channel.setId(2L);
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(2L);
        endpoint.setProtocol(Protocol.ANTHROPIC);
        endpoint.setEndpointUrl("https://api.anthropic.com");
        ChannelCredential cred = new ChannelCredential();
        cred.setApiKeyPlain("sk-ant");

        when(channelGateway.findAllActive()).thenReturn(List.of(channel));
        when(endpointGateway.findByChannelId(2L)).thenReturn(List.of(endpoint));
        when(credentialGateway.findActiveByChannelId(2L)).thenReturn(List.of(cred));
        when(clientRegistry.getClient(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("connect timeout"));

        probe().probe();

        verify(healthTracker).recordRequestResult("anthropic", false, "connect timeout");
    }

    @Test
    @DisplayName("无凭证的通道不应探活")
    void probe_noCredentialSkips() {
        Channel channel = new Channel();
        channel.setId(3L);
        ChannelEndpoint endpoint = new ChannelEndpoint();
        endpoint.setChannelId(3L);
        endpoint.setProtocol(Protocol.OPENAI);
        endpoint.setEndpointUrl("https://api.openai.com/v1");

        when(channelGateway.findAllActive()).thenReturn(List.of(channel));
        when(endpointGateway.findByChannelId(3L)).thenReturn(List.of(endpoint));
        when(credentialGateway.findActiveByChannelId(3L)).thenReturn(List.of());

        probe().probe();

        verify(healthTracker, never()).recordRequestResult(anyString(), org.mockito.ArgumentMatchers.anyBoolean(), anyString());
    }
}
