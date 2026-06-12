package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ModelInstanceDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型实例 Repository
 */
@Repository
public interface ModelInstanceRepository extends JpaRepository<ModelInstanceDo, Long> {

    List<ModelInstanceDo> findByChannelId(Long channelId);

    List<ModelInstanceDo> findByChannelIdAndPhase(Long channelId, String phase);

    List<ModelInstanceDo> findByModelIdAndPhase(Long modelId, String phase);

    List<ModelInstanceDo> findByModelIdAndPhaseOrderByPriorityAsc(Long modelId, String phase);

    List<ModelInstanceDo> findByIdIn(List<Long> ids);
}