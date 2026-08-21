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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.upstream.Protocol;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点持久化接口
 */
public interface ChannelEndpointGateway {

    /**
     * 保存端点
     */
    ChannelEndpoint save(ChannelEndpoint endpoint);

    /**
     * 根据 ID 查找端点
     */
    Optional<ChannelEndpoint> findById(Long id);

    /**
     * 根据渠道 ID 查找所有端点
     */
    List<ChannelEndpoint> findByChannelId(Long channelId);

    /**
     * 根据渠道 ID 和协议查找端点
     */
    Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol);

    /**
     * 查询所有端点
     */
    List<ChannelEndpoint> findAll();

    /**
     * 删除端点
     */
    void deleteById(Long id);
}
