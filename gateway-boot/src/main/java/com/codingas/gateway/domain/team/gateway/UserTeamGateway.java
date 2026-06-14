package com.codingas.gateway.domain.team.gateway;

import com.codingas.gateway.domain.team.entity.UserTeam;

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

    void updateRole(Long userId, Long teamId, String role);

    /**
     * 统计团队成员数
     */
    long countByTeamId(Long teamId);

    /**
     * 查找用户所属的团队 ID（业务层限制单团队，返回第一个）
     */
    Long findTeamIdByUserId(Long userId);
}
