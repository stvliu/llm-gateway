package com.codingas.gateway.infrastructure.supply.gateway.database.repository;

import com.codingas.gateway.infrastructure.supply.gateway.database.dataobject.ChannelModelDo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 渠道模型 Repository
 */
@Repository
public interface ChannelModelRepository extends JpaRepository<ChannelModelDo, Long> {

    List<ChannelModelDo> findByChannelId(Long channelId);

    List<ChannelModelDo> findByChannelIdAndState(Long channelId, String state);

    List<ChannelModelDo> findByModelSpecIdAndState(Long modelSpecId, String state);

    List<ChannelModelDo> findByIdIn(List<Long> ids);
}