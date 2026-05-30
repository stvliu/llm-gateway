package com.codingas.gateway.infrastructure.team.gateway.database.repository;

import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.TeamChannelDo;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.TeamChannelDo.TeamChannelId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 团队-渠道关联 JPA Repository
 */
public interface TeamChannelRepository extends JpaRepository<TeamChannelDo, TeamChannelId> {

    List<TeamChannelDo> findByTeamId(Long teamId);

    List<TeamChannelDo> findByChannelId(Long channelId);

    boolean existsByTeamIdAndChannelId(Long teamId, Long channelId);

    @Query("SELECT DISTINCT tc.channelId FROM TeamChannelDo tc WHERE tc.teamId = :teamId")
    List<Long> findChannelIdsByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM TeamChannelDo tc WHERE tc.teamId = :teamId")
    void deleteByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM TeamChannelDo tc WHERE tc.channelId = :channelId")
    void deleteByChannelId(@Param("channelId") Long channelId);
}
