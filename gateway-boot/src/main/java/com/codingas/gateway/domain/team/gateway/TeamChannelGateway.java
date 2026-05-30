package com.codingas.gateway.domain.team.gateway;

import com.codingas.gateway.domain.team.entity.TeamChannel;

import java.util.List;
import java.util.Optional;

/**
 * 团队-渠道关联网关接口
 * 
 * <p>提供团队与渠道多对多关系的持久化操作。</p>
 */
public interface TeamChannelGateway {

    /**
     * 保存团队-渠道关联
     */
    TeamChannel save(TeamChannel teamChannel);

    /**
     * 批量保存团队-渠道关联
     */
    List<TeamChannel> saveAll(List<TeamChannel> teamChannels);

    /**
     * 按团队 ID 查找所有关联的渠道
     */
    List<TeamChannel> findByTeamId(Long teamId);

    /**
     * 按渠道 ID 查找所有关联的团队
     */
    List<TeamChannel> findByChannelId(Long channelId);

    /**
     * 检查团队是否有权访问指定渠道
     */
    boolean existsByTeamIdAndChannelId(Long teamId, Long channelId);

    /**
     * 删除团队-渠道关联
     */
    void delete(TeamChannel teamChannel);

    /**
     * 删除团队的所有渠道关联
     */
    void deleteByTeamId(Long teamId);

    /**
     * 删除渠道的所有团队关联
     */
    void deleteByChannelId(Long channelId);

    /**
     * 查找团队关联的渠道 ID 列表
     */
    List<Long> findChannelIdsByTeamId(Long teamId);
}
