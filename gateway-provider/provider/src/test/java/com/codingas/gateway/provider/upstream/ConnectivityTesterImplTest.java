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
package com.codingas.gateway.provider.upstream;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ConnectivityTesterImpl 单元测试
 *
 * <p>覆盖测试委托逻辑：成功/失败结果透传、getClient 异常兜底、超时默认值与渠道覆盖。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectivityTesterImpl 单元测试")
class ConnectivityTesterImplTest {

    @Mock
    private UpstreamClientRegistry upstreamClientRegistry;
    @Mock
    private UpstreamClient<ProtocolRequest> upstreamClient;

    private ConnectivityTesterImpl tester;

    private static final Long CHANNEL_ID = 1L;

    @BeforeEach
    void setUp() {
        tester = new ConnectivityTesterImpl(upstreamClientRegistry);
    }

    private Channel stubChannel(Integer timeout) {
        Channel channel = new Channel();
        channel.setId(CHANNEL_ID);
        channel.setName("test-channel");
        channel.setTimeout(timeout);
        return channel;
    }

    @Test
    @DisplayName("client 返回成功 → 原样透传成功结果")
    void 成功结果透传() {
        when(upstreamClientRegistry.getClient(anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(upstreamClient);
        when(upstreamClient.testConnectivity())
                .thenReturn(ConnectivityTestResult.success(CHANNEL_ID, 50L));

        ConnectivityTestResult result = tester.test(stubChannel(15));

        assertThat(result.success()).isTrue();
        assertThat(result.channelId()).isEqualTo(CHANNEL_ID);
        assertThat(result.latencyMs()).isEqualTo(50L);
        // 渠道配置了 timeout → 透传给 registry
        verify(upstreamClientRegistry).getClient("openai", "", null, 15);
    }

    @Test
    @DisplayName("client 返回失败 → 原样透传失败结果")
    void 失败结果透传() {
        when(upstreamClientRegistry.getClient(anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(upstreamClient);
        when(upstreamClient.testConnectivity())
                .thenReturn(ConnectivityTestResult.failure(CHANNEL_ID, "connection refused"));

        ConnectivityTestResult result = tester.test(stubChannel(15));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("connection refused");
    }

    @Test
    @DisplayName("getClient 异常 → 返回失败结果并携带异常信息")
    void getClient异常兜底() {
        when(upstreamClientRegistry.getClient(anyString(), anyString(), isNull(), anyInt()))
                .thenThrow(new RuntimeException("registry broken"));

        ConnectivityTestResult result = tester.test(stubChannel(15));

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("registry broken");
    }

    @Test
    @DisplayName("渠道未配置 timeout → 使用默认 30 秒")
    void 默认超时30秒() {
        when(upstreamClientRegistry.getClient(anyString(), anyString(), isNull(), anyInt()))
                .thenReturn(upstreamClient);
        when(upstreamClient.testConnectivity())
                .thenReturn(ConnectivityTestResult.success(CHANNEL_ID, 10L));

        tester.test(stubChannel(null));

        verify(upstreamClientRegistry).getClient("openai", "", null, 30);
    }
}
