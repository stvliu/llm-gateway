package com.codingas.gateway.infrastructure.team.gateway;

import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.enums.TeamState;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.TeamDo;
import com.codingas.gateway.infrastructure.team.gateway.database.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 团队 Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TeamGatewayImpl implements TeamGateway {

    private final TeamRepository teamRepository;

    @Override
    public Team save(Team team) {
        TeamDo dataObject = toDataObject(team);
        if (team.getId() == null) {
            dataObject.setCreatedAt(LocalDateTime.now());
        }
        dataObject.setUpdatedAt(LocalDateTime.now());
        TeamDo saved = teamRepository.save(dataObject);
        return toEntity(saved);
    }

    @Override
    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id).map(this::toEntity);
    }

    @Override
    public List<Team> findAllActive() {
        return teamRepository.findAllActive().stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public void deleteById(Long id) {
        teamRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }

    private Team toEntity(TeamDo dataObject) {
        Team entity = new Team();
        entity.setId(dataObject.getId());
        entity.setName(dataObject.getName());
        entity.setDescription(dataObject.getDescription());
        entity.setState(TeamState.fromCode(dataObject.getState()));
        return entity;
    }

    private TeamDo toDataObject(Team entity) {
        TeamDo dataObject = new TeamDo();
        dataObject.setId(entity.getId());
        dataObject.setName(entity.getName());
        dataObject.setDescription(entity.getDescription());
        dataObject.setState(entity.getState().getCode());
        return dataObject;
    }
}
