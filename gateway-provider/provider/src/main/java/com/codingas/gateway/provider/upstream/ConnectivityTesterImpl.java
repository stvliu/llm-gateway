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
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 连通性测试实现
 *
 * <p>基于 UpstreamClientRegistry 执行分层连通性测试。</p>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConnectivityTesterImpl implements ConnectivityTester {

    private final UpstreamClientRegistry upstreamClientRegistry;

    @Override
    public ConnectivityTestResult test(Channel channel) {
        // TODO: Channel 已不再持有 endpointUrl/protocol，需要通过 ChannelEndpoint 获取
        // 将在后续 Task 中通过 ChannelEndpointGateway 重构此方法
        try {
            var client = upstreamClientRegistry.getClient(
                    "openai",
                    "",
                    null,
                    channel.getTimeout() != null ? channel.getTimeout() : 30
            );
            return client.testConnectivity();
        } catch (Exception e) {
            log.error("连通性测试失败, channelId={}", channel.getId(), e);
            return ConnectivityTestResult.failure(channel.getId(), e.getMessage());
        }
    }
}
