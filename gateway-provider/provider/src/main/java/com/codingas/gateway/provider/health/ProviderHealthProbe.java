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
package com.codingas.gateway.provider.health;

import com.codingas.gateway.provider.channel.Channel;
import com.codingas.gateway.provider.channel.ChannelCredential;
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.channel.ChannelCredentialRepository;
import com.codingas.gateway.provider.channel.ChannelEndpointRepository;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.protocol.ProtocolRequest;
import com.codingas.gateway.protocol.transport.UpstreamClient;
import com.codingas.gateway.protocol.transport.UpstreamClientRegistry;
import com.codingas.gateway.protocol.transport.ConnectivityTestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider 健康周期探活（主动探测）。
 *
 * <p><b>4.5 健康机制</b>：与 {@link ProviderHealthTracker} 的被动推断互补，
 * 本组件按 {@code gateway.health.provider.probe-interval} 周期主动探测各启用通道
 * 的上游连通性，并把结果归并到供应商级健康状态（按协议名），供 actuator
 * {@link ProviderRegistryHealthIndicator} 暴露。</p>
 *
 * <p>探活数据链：启用通道 → 端点（协议+URL）→ 凭证（API Key）→ 构造
 * {@link UpstreamClient} 并 {@code testConnectivity()}。任一环节缺失或异常按失败记录。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderHealthProbe {

    private final ChannelRepository channelRepository;
    private final ChannelEndpointRepository endpointRepository;
    private final ChannelCredentialRepository credentialRepository;
    private final UpstreamClientRegistry clientRegistry;
    private final ProviderHealthTracker healthTracker;

    /**
     * 周期探活：遍历启用通道，探测上游连通性并更新供应商级健康状态。
     */
    @Scheduled(
            fixedDelayString = "${gateway.health.provider.probe-interval:30s}",
            initialDelayString = "${gateway.health.provider.probe-initial-delay:5s}")
    public void probe() {
        List<Channel> channels = channelRepository.findAllActive();
        for (Channel channel : channels) {
            probeChannel(channel);
        }
        log.debug("完成一次 Provider 健康周期探活，通道数={}", channels.size());
    }

    /** 探测单个通道的所有端点 */
    private void probeChannel(Channel channel) {
        for (ChannelEndpoint endpoint : endpointRepository.findByChannelId(channel.getId())) {
            probeEndpoint(channel, endpoint);
        }
    }

    /** 探测单个端点（用其凭证构造上游客户端并测试连通性） */
    private void probeEndpoint(Channel channel, ChannelEndpoint endpoint) {
        String protocol = endpoint.getProtocol() == null ? null : endpoint.getProtocol().name().toLowerCase();
        if (protocol == null) {
            return;
        }
        credentialRepository.findActiveByChannelId(channel.getId()).stream().findFirst()
                .ifPresent(cred -> {
                    boolean success;
                    String error;
                    try {
                        UpstreamClient<ProtocolRequest> client = clientRegistry.getClient(
                                protocol, endpoint.getEndpointUrl(),
                                cred.getApiKeyPlain(), channel.getTimeout() == null ? 30 : channel.getTimeout());
                        ConnectivityTestResult result = client.testConnectivity();
                        success = result.success();
                        error = result.errorMessage();
                    } catch (Exception e) {
                        success = false;
                        error = e.getMessage();
                    }
                    healthTracker.recordRequestResult(protocol, success, error);
                });
    }
}
