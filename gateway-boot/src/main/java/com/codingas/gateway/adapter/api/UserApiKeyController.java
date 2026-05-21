package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateRequest;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyCreateResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyDetailResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyResponse;
import com.codingas.gateway.application.userapikey.dto.UserApiKeyUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 API Key 管理接口
 */
@RestController
@RequestMapping("/api/user-api-keys")
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;

    public UserApiKeyController(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @PostMapping
    public ResponseEntity<UserApiKeyCreateResponse> create(
            @Valid @RequestBody UserApiKeyCreateRequest request) {
        return ResponseEntity.ok(userApiKeyService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserApiKeyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userApiKeyService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<UserApiKeyDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(userApiKeyService.getDetailById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserApiKeyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserApiKeyUpdateRequest request) {
        return ResponseEntity.ok(userApiKeyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userApiKeyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<UserApiKeyResponse>> listByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(userApiKeyService.listByTeamId(teamId));
    }
}
