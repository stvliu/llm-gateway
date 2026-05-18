package com.codingas.gateway.infrastructure.team.gateway;

import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.enums.TeamRole;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import com.codingas.gateway.infrastructure.team.gateway.database.dataobject.UserTeamDo;
import com.codingas.gateway.infrastructure.team.gateway.database.repository.UserTeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户-团队关联 Gateway 实现
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserTeamGatewayImpl implements UserTeamGateway {

    private final UserTeamRepository userTeamRepository;

    @Override
    public UserTeam save(UserTeam userTeam) {
        UserTeamDo dataObject = toDataObject(userTeam);
        dataObject.setCreatedAt(LocalDateTime.now());
        userTeamRepository.save(dataObject);
        return userTeam;
    }

    @Override
    public List<UserTeam> findByUserId(Long userId) {
        return userTeamRepository.findByUserId(userId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public List<UserTeam> findByTeamId(Long teamId) {
        return userTeamRepository.findByTeamId(teamId).stream()
            .map(this::toEntity)
            .toList();
    }

    @Override
    public boolean isMember(Long userId, Long teamId) {
        return userTeamRepository.isMember(userId, teamId);
    }

    @Override
    public void removeMember(Long userId, Long teamId) {
        userTeamRepository.deleteByUserIdAndTeamId(userId, teamId);
    }

    @Override
    public void updateRole(Long userId, Long teamId, TeamRole role) {
        userTeamRepository.updateRole(userId, teamId, role.getCode());
    }

    private UserTeam toEntity(UserTeamDo dataObject) {
        UserTeam entity = new UserTeam();
        entity.setUserId(dataObject.getUserId());
        entity.setTeamId(dataObject.getTeamId());
        entity.setRole(TeamRole.fromCode(dataObject.getRole()));
        return entity;
    }

    private UserTeamDo toDataObject(UserTeam entity) {
        UserTeamDo dataObject = new UserTeamDo();
        dataObject.setUserId(entity.getUserId());
        dataObject.setTeamId(entity.getTeamId());
        dataObject.setRole(entity.getRole().getCode());
        return dataObject;
    }
}
