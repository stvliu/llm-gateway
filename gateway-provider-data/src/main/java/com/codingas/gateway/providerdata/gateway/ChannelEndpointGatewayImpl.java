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
package com.codingas.gateway.providerdata.gateway;

import com.codingas.gateway.provider.channel.ChannelEndpoint;
import com.codingas.gateway.provider.upstream.Protocol;
import com.codingas.gateway.provider.channel.ChannelEndpointGateway;
import com.codingas.gateway.providerdata.dataobject.ChannelEndpointDo;
import com.codingas.gateway.providerdata.repository.ChannelEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 渠道端点持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelEndpointGatewayImpl implements ChannelEndpointGateway {

    private final ChannelEndpointRepository channelEndpointRepository;

    @Override
    public ChannelEndpoint save(ChannelEndpoint endpoint) {
        ChannelEndpointDo doObj = toDo(endpoint);
        ChannelEndpointDo saved = channelEndpointRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<ChannelEndpoint> findById(Long id) {
        return channelEndpointRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<ChannelEndpoint> findByChannelId(Long channelId) {
        return channelEndpointRepository.findByChannelId(channelId)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<ChannelEndpoint> findByChannelIdAndProtocol(Long channelId, Protocol protocol) {
        return channelEndpointRepository.findByChannelIdAndProtocol(channelId, protocol)
                .map(this::toEntity);
    }

    @Override
    public List<ChannelEndpoint> findAll() {
        return channelEndpointRepository.findAll()
                .stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        channelEndpointRepository.deleteById(id);
    }

    private ChannelEndpoint toEntity(ChannelEndpointDo doObj) {
        ChannelEndpoint entity = new ChannelEndpoint();
        entity.setId(doObj.getId());
        entity.setChannelId(doObj.getChannelId());
        entity.setProtocol(doObj.getProtocol());
        entity.setEndpointUrl(doObj.getEndpointUrl());
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ChannelEndpointDo toDo(ChannelEndpoint entity) {
        ChannelEndpointDo doObj = new ChannelEndpointDo();
        doObj.setId(entity.getId());
        doObj.setChannelId(entity.getChannelId());
        doObj.setProtocol(entity.getProtocol());
        doObj.setEndpointUrl(entity.getEndpointUrl());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}