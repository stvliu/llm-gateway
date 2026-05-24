package com.codingas.gateway.adapter.api;

import cn.dev33.satoken.stp.StpUtil;
import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.team.dto.TeamRequest;
import com.codingas.gateway.application.team.dto.TeamResponse;
import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import com.codingas.gateway.domain.team.entity.UserTeam;
import com.codingas.gateway.domain.team.enums.TeamRole;
import com.codingas.gateway.domain.team.gateway.UserTeamGateway;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        TeamResponse response = teamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        TeamResponse response = teamService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getById(@PathVariable Long id) {
        TeamResponse response = teamService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> listAll() {
        List<TeamResponse> responses = teamService.listAll();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long teamId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "member") String role) {
        teamService.addMember(teamId, userId, TeamRole.fromCode(role));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        teamService.removeMember(teamId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestParam String role) {
        teamService.updateMemberRole(teamId, userId, TeamRole.fromCode(role));
        return ResponseEntity.ok().build();
    }

    // ==================== UserApiKey 子资源 ====================

    /**
     * 查询团队下所有用户的 API Key（通过团队成员关系查找）
     */
    @GetMapping("/{teamId}/api-keys")
    public List<UserApiKeyResponse> listApiKeys(@PathVariable Long teamId) {
        // 查找团队中所有用户，再查找他们的 Key
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
    public ResponseEntity<UserApiKeyCreateResponse> createApiKey(
            @PathVariable Long teamId,
            @Valid @RequestBody UserApiKeyCreateRequest request) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = StpUtil.hasRole("ADMIN");

        Long targetUserId = request.userId() != null ? request.userId() : currentUserId;
        if (!isAdmin && !targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("无权为其他用户创建 API Key");
        }

        UserApiKeyCreateRequest fixedRequest = new UserApiKeyCreateRequest(
                targetUserId, request.channelIds(), request.name(),
                request.models(), request.quotaLimit()
        );
        UserApiKeyCreateResponse response = userApiKeyService.create(fixedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    public ResponseEntity<Void> deleteApiKey(@PathVariable Long id) {
        userApiKeyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
