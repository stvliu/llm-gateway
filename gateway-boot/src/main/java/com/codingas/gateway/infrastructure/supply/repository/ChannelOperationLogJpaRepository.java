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
