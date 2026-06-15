package com.codingas.gateway.application.team;

import com.codingas.gateway.application.team.dto.TeamRequest;
import com.codingas.gateway.application.team.dto.TeamResponse;
import java.util.List;

/**
 * 团队应用服务接口
 */
public interface TeamService {

    TeamResponse create(TeamRequest request);

    TeamResponse update(Long id, TeamRequest request);

    TeamResponse getById(Long id);

    List<TeamResponse> listAll();

    void delete(Long id);

    void addMember(Long teamId, Long userId, String role);

    void removeMember(Long teamId, Long userId);

    void updateMemberRole(Long teamId, Long userId, String role);
}
