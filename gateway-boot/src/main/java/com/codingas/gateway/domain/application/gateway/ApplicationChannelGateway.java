package com.codingas.gateway.domain.application.gateway;

import com.codingas.gateway.domain.application.entity.ApplicationChannel;

import java.util.List;
import java.util.Set;

/**
 * 应用-渠道授权关联领域网关接口
 *
 * <p>决定应用可见的渠道集合。domain 层仅依赖此接口，
 * 实现位于 infrastructure 层（COLA Light 依赖倒置）。</p>
 */
public interface ApplicationChannelGateway {

    /**
     * 查询应用可见的渠道 ID 集合（去重）
     *
     * @param appId 应用 ID
     * @return 渠道 ID 集合
     */
    Set<Long> findChannelIdsByApplicationId(Long appId);

    /**
     * 查询应用下的全部授权关联
     *
     * @param appId 应用 ID
     * @return 关联列表
     */
    List<ApplicationChannel> findByApplicationId(Long appId);

    /**
     * 批量保存授权关联
     *
     * @param rels 关联列表
     */
    void saveAll(List<ApplicationChannel> rels);

    /**
     * 判断应用-渠道授权关联是否存在
     *
     * @param appId 应用 ID
     * @param chId  渠道 ID
     * @return 存在返回 true，否则 false
     */
    boolean existsByApplicationIdAndChannelId(Long appId, Long chId);
}
