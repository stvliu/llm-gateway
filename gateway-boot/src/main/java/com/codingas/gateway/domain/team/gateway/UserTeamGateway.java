package com.codingas.gateway.domain.team.gateway;

import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.enums.TeamRole;

import java.util.List;

/**
 * 用户-团队关联 Gateway 接口
 */
public interface UserTeamGateway {

    UserTeam save(UserTeam userTeam);

    List<UserTeam> findByUserId(Long userId);

    List<UserTeam> findByTeamId(Long teamId);

    boolean isMember(Long userId, Long teamId);

    void removeMember(Long userId, Long teamId);

    void updateRole(Long userId, Long teamId, TeamRole role);
}
