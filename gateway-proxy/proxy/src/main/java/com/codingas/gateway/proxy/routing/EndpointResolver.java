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
import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.protocol.Protocol;
import com.codingas.gateway.provider.channel.ChannelEndpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 端点解析器 — 根据 channelId 和协议查找可用端点
 */
@Component
@RequiredArgsConstructor
public class EndpointResolver {

    private final ChannelEndpointRepository channelEndpointRepository;

    /**
     * 根据 channelId 和入站协议解析可用的端点
     *
     * <p>优先匹配协议同源的端点，避免不必要的跨协议转换。
     * 如果找不到匹配协议的同源端点，回退到任意可用端点。</p>
     *
     * @param channelId 渠道 ID
     * @param protocol  入站协议
     * @return 可用的 ChannelEndpoint
     * @throws ResourceNotFoundException 未找到可用端点
     */
    public ChannelEndpoint resolve(Long channelId, Protocol protocol) {
        // 优先匹配协议同源的端点
        return channelEndpointRepository.findByChannelIdAndProtocol(channelId, protocol)
                .orElseGet(() -> {
                    List<ChannelEndpoint> endpoints = channelEndpointRepository.findByChannelId(channelId);
                    return endpoints.stream()
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("ChannelEndpoint", channelId));
                });
    }
}
