package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.team.dto.AddTeamMemberRequest;
import com.codingas.gateway.application.team.dto.TeamRequest;
import com.codingas.gateway.application.team.dto.TeamResponse;
import com.codingas.gateway.application.team.dto.UpdateMemberRoleRequest;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.team.entity.TeamChannel;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.gateway.TeamChannelGateway;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 团队管理 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserApiKeyService userApiKeyService;
    private final UserTeamGateway userTeamGateway;
    private final TeamChannelGateway teamChannelGateway;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(@Valid @RequestBody TeamRequest request) {
        return teamService.create(request);
    }

    @PutMapping("/{id}")
    public TeamResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        return teamService.update(id, request);
    }

    @GetMapping("/{id}")
    public TeamResponse getById(@PathVariable Long id) {
        return teamService.getById(id);
    }

    @GetMapping
    public List<TeamResponse> listAll() {
        return teamService.listAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        teamService.delete(id);
    }

    @PostMapping("/{teamId}/members")
    public void addMember(
            @PathVariable Long teamId,
            @Valid @RequestBody AddTeamMemberRequest request) {
        teamService.addMember(teamId, request.getUserId(), request.getRole());
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        teamService.removeMember(teamId, userId);
    }

    @PutMapping("/{teamId}/members/{userId}/role")
    public void updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        teamService.updateMemberRole(teamId, userId, request.getRole());
    }

    // ==================== UserApiKey 子资源 ====================

    /**
     * 查询团队下所有用户的 API Key（通过团队成员关系查找）
     */
    @GetMapping("/{teamId}/api-keys")
    public List<UserApiKeyResponse> listApiKeys(@PathVariable Long teamId) {
        List<UserTeam> members = userTeamGateway.findByTeamId(teamId);
        List<Long> userIds = members.stream().map(UserTeam::getUserId).toList();
        return userIds.stream()
                .flatMap(userId -> userApiKeyService.findByUserId(userId).stream())
                .toList();
    }

    /**
     * 查询单个 API Key（含明文）
     */
    @GetMapping("/{teamId}/api-keys/{id}")
    public UserApiKeyDetailResponse getApiKey(@PathVariable Long id) {
        return userApiKeyService.getDetailById(id);
    }

    /**
     * 创建用户 API Key
     */
    @PostMapping("/{teamId}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public UserApiKeyCreateResponse createApiKey(
            @PathVariable Long teamId,
            @Valid @RequestBody UserApiKeyCreateRequest request) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("ADMIN");

        Long targetUserId = request.userId() != null ? request.userId() : currentUserId;
        if (!isAdmin && !targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("无权为其他用户创建 API Key");
        }

        UserApiKeyCreateRequest fixedRequest = new UserApiKeyCreateRequest(
                targetUserId, request.name()
        );
        return userApiKeyService.create(fixedRequest);
    }

    /**
     * 更新用户 API Key
     */
    @PutMapping("/{teamId}/api-keys/{id}")
    public UserApiKeyResponse updateApiKey(
            @PathVariable Long id,
            @Valid @RequestBody UserApiKeyUpdateRequest request) {
        return userApiKeyService.update(id, request);
    }

    /**
     * 删除用户 API Key
     */
    @DeleteMapping("/{teamId}/api-keys/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiKey(@PathVariable Long id) {
        userApiKeyService.delete(id);
    }

    // ==================== 渠道管理 ====================

    /**
     * 获取团队关联的渠道 ID 列表
     */
    @SaCheckRole("ADMIN")
    @GetMapping("/{teamId}/channels")
    public List<Long> getTeamChannels(@PathVariable Long teamId) {
        return teamChannelGateway.findChannelIdsByTeamId(teamId);
    }

    /**
     * 更新团队关联的渠道（全量替换）
     */
    @SaCheckRole("ADMIN")
    @PutMapping("/{teamId}/channels")
    @Transactional
    public void updateTeamChannels(
            @PathVariable Long teamId,
            @RequestBody TeamChannelsUpdateRequest request) {
        teamChannelGateway.deleteByTeamId(teamId);
        for (Long channelId : request.channelIds()) {
            teamChannelGateway.save(new TeamChannel(teamId, channelId));
        }
    }

    /**
     * 团队渠道更新请求
     */
    record TeamChannelsUpdateRequest(List<Long> channelIds) {}
}
