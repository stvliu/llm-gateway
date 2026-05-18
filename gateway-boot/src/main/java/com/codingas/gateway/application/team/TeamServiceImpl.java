package com.codingas.gateway.application.team;

import com.codingas.gateway.application.team.dto.TeamRequest;
import com.codingas.gateway.application.team.dto.TeamResponse;
import com.codingas.gateway.domain.team.entity.Team;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.enums.TeamRole;
import com.codingas.gateway.domain.team.exception.TeamNotFoundException;
import com.codingas.gateway.domain.team.gateway.TeamGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 团队应用服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamGateway teamGateway;
    private final UserTeamGateway userTeamGateway;

    @Override
    @Transactional
    public TeamResponse create(TeamRequest request) {
        if (teamGateway.existsByName(request.getName())) {
            throw new IllegalArgumentException("团队名称已存在: " + request.getName());
        }

        Team team = new Team();
        team.setName(request.getName());
        team.setDescription(request.getDescription());

        Team saved = teamGateway.save(team);
        log.info("Created team: id={}, name={}", saved.getId(), saved.getName());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        Team team = teamGateway.findById(id)
            .orElseThrow(() -> new TeamNotFoundException(id));

        if (!team.getName().equals(request.getName())) {
            if (teamGateway.existsByName(request.getName())) {
                throw new IllegalArgumentException("团队名称已存在: " + request.getName());
            }
        }

        team.setName(request.getName());
        team.setDescription(request.getDescription());

        Team saved = teamGateway.save(team);
        log.info("Updated team: id={}", saved.getId());

        return toResponse(saved);
    }

    @Override
    public TeamResponse getById(Long id) {
        Team team = teamGateway.findById(id)
            .orElseThrow(() -> new TeamNotFoundException(id));

        TeamResponse response = toResponse(team);

        List<UserTeam> members = userTeamGateway.findByTeamId(id);
        response.setMembers(members.stream()
            .map(ut -> {
                TeamResponse.MemberResponse mr = new TeamResponse.MemberResponse();
                mr.setUserId(ut.getUserId());
                mr.setRole(ut.getRole().getCode());
                return mr;
            })
            .toList());

        return response;
    }

    @Override
    public List<TeamResponse> listAll() {
        return teamGateway.findAllActive().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        teamGateway.deleteById(id);
        log.info("Deleted team: id={}", id);
    }

    @Override
    @Transactional
    public void addMember(Long teamId, Long userId, TeamRole role) {
        if (userTeamGateway.isMember(userId, teamId)) {
            throw new IllegalArgumentException("用户已是团队成员");
        }

        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setRole(role);

        userTeamGateway.save(userTeam);
        log.info("Added member: userId={}, teamId={}, role={}", userId, teamId, role);
    }

    @Override
    @Transactional
    public void removeMember(Long teamId, Long userId) {
        userTeamGateway.removeMember(userId, teamId);
        log.info("Removed member: userId={}, teamId={}", userId, teamId);
    }

    @Override
    @Transactional
    public void updateMemberRole(Long teamId, Long userId, TeamRole role) {
        userTeamGateway.updateRole(userId, teamId, role);
        log.info("Updated member role: userId={}, teamId={}, role={}", userId, teamId, role);
    }

    private TeamResponse toResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setState(team.getState().getCode());
        return response;
    }
}
