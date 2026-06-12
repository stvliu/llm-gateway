package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.Channel;
import com.codingas.gateway.domain.supply.enums.BillingMode;
import com.codingas.gateway.domain.supply.gateway.ChannelGateway;
import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelDo;
import com.codingas.gateway.infrastructure.supply.gateway.database.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 渠道持久化实现
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChannelGatewayImpl implements ChannelGateway {

    private final ChannelRepository channelRepository;

    @Override
    public Channel save(Channel channel) {
        ChannelDo doObj = toDo(channel);
        ChannelDo saved = channelRepository.save(doObj);
        return toEntity(saved);
    }

    @Override
    public Optional<Channel> findById(Long id) {
        return channelRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<Channel> findByProviderId(Long providerId) {
        return channelRepository.findByProviderId(providerId).stream().map(this::toEntity).toList();
    }

    @Override
    public List<Channel> findAllActive() {
        return channelRepository.findByPhase(Channel.Phase.ACTIVE.name()).stream().map(this::toEntity).toList();
    }

    @Override
    public List<Channel> findAll() {
        return channelRepository.findAll().stream().map(this::toEntity).toList();
    }

    @Override
    public void deleteById(Long id) {
        channelRepository.deleteById(id);
    }

    @Override
    public List<Channel> findByIds(List<Long> ids) {
        return channelRepository.findByIdIn(ids).stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByProviderIdAndName(Long providerId, String name) {
        return channelRepository.existsByProviderIdAndName(providerId, name);
    }

    @Override
    public List<Channel> findByProviderIdAndBillingMode(Long providerId, BillingMode billingMode) {
        return channelRepository.findByProviderIdAndBillingMode(providerId, billingMode)
                .stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<Channel> findByProviderIdAndName(Long providerId, String name) {
        return channelRepository.findByProviderIdAndName(providerId, name).map(this::toEntity);
    }

    private Channel toEntity(ChannelDo doObj) {
        Channel entity = new Channel();
        entity.setId(doObj.getId());
        entity.setProviderId(doObj.getProviderId());
        entity.setName(doObj.getName());
        entity.setBillingMode(doObj.getBillingMode());
        entity.setQuotaLimit(doObj.getQuotaLimit());
        entity.setTimeout(doObj.getTimeout());
        entity.setMaxRetries(doObj.getMaxRetries());
        entity.setPhase(Channel.Phase.valueOf(doObj.getPhase()));
        entity.setCreatedBy(doObj.getCreatedBy());
        entity.setUpdatedBy(doObj.getUpdatedBy());
        entity.setCreatedAt(doObj.getCreatedAt());
        entity.setUpdatedAt(doObj.getUpdatedAt());
        return entity;
    }

    private ChannelDo toDo(Channel entity) {
        ChannelDo doObj = new ChannelDo();
        doObj.setId(entity.getId());
        doObj.setProviderId(entity.getProviderId());
        doObj.setName(entity.getName());
        doObj.setBillingMode(entity.getBillingMode());
        doObj.setQuotaLimit(entity.getQuotaLimit());
        doObj.setTimeout(entity.getTimeout());
        doObj.setMaxRetries(entity.getMaxRetries());
        doObj.setPhase(entity.getPhase() != null ? entity.getPhase().name() : Channel.Phase.ACTIVE.name());
        doObj.setCreatedBy(entity.getCreatedBy());
        doObj.setUpdatedBy(entity.getUpdatedBy());
        return doObj;
    }
}