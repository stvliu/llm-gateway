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
package com.codingas.gateway.infrastructure.supply.repository;

import com.codingas.gateway.infrastructure.supply.repository.ChannelOperationLogJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 渠道操作日志 JPA 仓储
 */
@Repository
public interface ChannelOperationLogJpaRepository extends JpaRepository<ChannelOperationLogJpaEntity, Long> {

    List<ChannelOperationLogJpaEntity> findByChannelIdOrderByOperatedAtDesc(Long channelId, Pageable pageable);

    List<ChannelOperationLogJpaEntity> findByChannelIdAndActionInOrderByOperatedAtDesc(
            Long channelId, List<String> actions, Pageable pageable);

    long countByChannelId(Long channelId);

    List<ChannelOperationLogJpaEntity> findByOperatorIdOrderByOperatedAtDesc(Long operatorId, Pageable pageable);

    List<ChannelOperationLogJpaEntity> findByBatchIdOrderByOperatedAtDesc(Long batchId);
}
