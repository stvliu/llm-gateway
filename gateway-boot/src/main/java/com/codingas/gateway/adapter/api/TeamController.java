package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.team.TeamService;
import com.codingas.gateway.application.team.dto.TeamRequest;
import com.codingas.gateway.application.team.dto.TeamResponse;
import com.codingas.gateway.domain.team.enums.TeamRole;
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
}
