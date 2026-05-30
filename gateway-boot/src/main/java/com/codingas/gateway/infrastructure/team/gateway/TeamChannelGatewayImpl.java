package com.codingas.gateway.infrastructure.team.gateway;

import com.codingas.gateway.domain.team.entity.TeamChannel;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.TeamChannelDo;
import com.codingas.gateway.infrastructure.team.gateway.database.repository.TeamChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 团队-渠道关联 Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeamChannelGatewayImpl implements TeamChannelGateway {

    private final TeamChannelRepository teamChannelRepository;

    @Override
    public TeamChannel save(TeamChannel teamChannel) {
        TeamChannelDo dataObject = toDataObject(teamChannel);
        teamChannelRepository.save(dataObject);
        return teamChannel;
    }

    @Override
    public List<TeamChannel> saveAll(List<TeamChannel> teamChannels) {
        List<TeamChannelDo> dataObjects = teamChannels.stream().map(this::toDataObject).toList();
        teamChannelRepository.saveAll(dataObjects);
        return teamChannels;
    }

    @Override
    public List<TeamChannel> findByTeamId(Long teamId) {
        return teamChannelRepository.findByTeamId(teamId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public List<TeamChannel> findByChannelId(Long channelId) {
        return teamChannelRepository.findByChannelId(channelId).stream()
                .map(this::toEntity)
                .toList();
    }

    @Override
    public boolean existsByTeamIdAndChannelId(Long teamId, Long channelId) {
        return teamChannelRepository.existsByTeamIdAndChannelId(teamId, channelId);
    }

    @Override
    public void delete(TeamChannel teamChannel) {
        TeamChannelDo.TeamChannelId id = new TeamChannelDo.TeamChannelId();
        id.setTeamId(teamChannel.getTeamId());
        id.setChannelId(teamChannel.getChannelId());
        teamChannelRepository.deleteById(id);
    }

    @Override
    public void deleteByTeamId(Long teamId) {
        teamChannelRepository.deleteByTeamId(teamId);
    }

    @Override
    public void deleteByChannelId(Long channelId) {
        teamChannelRepository.deleteByChannelId(channelId);
    }

    @Override
    public List<Long> findChannelIdsByTeamId(Long teamId) {
        return teamChannelRepository.findChannelIdsByTeamId(teamId);
    }

    private TeamChannel toEntity(TeamChannelDo dataObject) {
        TeamChannel entity = new TeamChannel();
        entity.setTeamId(dataObject.getTeamId());
        entity.setChannelId(dataObject.getChannelId());
        entity.setCreatedAt(dataObject.getCreatedAt());
        return entity;
    }

    private TeamChannelDo toDataObject(TeamChannel entity) {
        TeamChannelDo dataObject = new TeamChannelDo();
        dataObject.setTeamId(entity.getTeamId());
        dataObject.setChannelId(entity.getChannelId());
        dataObject.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : Instant.now());
        return dataObject;
    }
}
