package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelEndpoint;
import com.codingas.gateway.domain.supply.enums.ChannelEndpointState;
import com.codingas.gateway.domain.supply.enums.Protocol;
import com.codingas.gateway.domain.supply.gateway.ChannelEndpointGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelEndpointDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelEndpointRepository;
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
    public List<ChannelEndpoint> findActiveByChannelId(Long channelId) {
        return channelEndpointRepository.findByChannelIdAndState(channelId, ChannelEndpointState.ACTIVE)
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
        entity.setState(doObj.getState());
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
        doObj.setState(entity.getState() != null ? entity.getState() : ChannelEndpointState.ACTIVE);
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}
