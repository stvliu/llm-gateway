/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.infrastructure.supply.gateway;

import com.codingas.gateway.domain.supply.entity.ChannelOperationLog;
import com.codingas.gateway.domain.supply.gateway.ChannelOperationLogGateway;
import com.codingas.gateway.infrastructure.supply.repository.ChannelOperationLogJpaEntity;
import com.codingas.gateway.infrastructure.supply.repository.ChannelOperationLogJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 渠道操作日志 Gateway 实现
 */
@Component
public class ChannelOperationLogGatewayImpl implements ChannelOperationLogGateway {

    private final ChannelOperationLogJpaRepository repository;

    public ChannelOperationLogGatewayImpl(ChannelOperationLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(ChannelOperationLog log) {
        ChannelOperationLogJpaEntity entity = ChannelOperationLogJpaEntity.fromDomain(log);
        repository.save(entity);
        log.setId(entity.getId());
    }

    @Override
    public void saveAll(List<ChannelOperationLog> logs) {
        List<ChannelOperationLogJpaEntity> entities = logs.stream()
                .map(ChannelOperationLogJpaEntity::fromDomain)
                .collect(Collectors.toList());
        repository.saveAll(entities);
    }

    @Override
    public Optional<ChannelOperationLog> findById(Long id) {
        return repository.findById(id)
                .map(ChannelOperationLogJpaEntity::toDomain);
    }

    @Override
    public List<ChannelOperationLog> findByChannelId(Long channelId, int page, int size) {
        return repository.findByChannelIdOrderByOperatedAtDesc(channelId, PageRequest.of(page, size))
                .stream()
                .map(ChannelOperationLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelOperationLog> findByChannelIdAndActions(Long channelId, List<String> actions, int page, int size) {
        return repository.findByChannelIdAndActionInOrderByOperatedAtDesc(channelId, actions, PageRequest.of(page, size))
                .stream()
                .map(ChannelOperationLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByChannelId(Long channelId) {
        return repository.countByChannelId(channelId);
    }

    @Override
    public List<ChannelOperationLog> findByOperatorId(Long operatorId, int page, int size) {
        return repository.findByOperatorIdOrderByOperatedAtDesc(operatorId, PageRequest.of(page, size))
                .stream()
                .map(ChannelOperationLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChannelOperationLog> findByBatchId(Long batchId) {
        return repository.findByBatchIdOrderByOperatedAtDesc(batchId)
                .stream()
                .map(ChannelOperationLogJpaEntity::toDomain)
                .collect(Collectors.toList());
    }
}
