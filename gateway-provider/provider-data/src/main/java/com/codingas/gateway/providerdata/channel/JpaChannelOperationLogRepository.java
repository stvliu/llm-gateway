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
package com.codingas.gateway.providerdata.channel;

import com.codingas.gateway.provider.channel.ChannelOperationLog;
import com.codingas.gateway.provider.channel.ChannelOperationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 渠道操作日志 Gateway 实现
 */
@Component
public class JpaChannelOperationLogRepository implements ChannelOperationLogRepository {

    private final ChannelOperationLogJpaRepository repository;

    public JpaChannelOperationLogRepository(ChannelOperationLogJpaRepository repository) {
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
