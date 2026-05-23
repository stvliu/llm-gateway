package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.userapikey.UserApiKeyService;
import com.codingas.gateway.application.userapikey.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户 API Key 管理 API
 */
@RestController
@RequestMapping("/api/user-api-keys")
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;

    public UserApiKeyController(UserApiKeyService userApiKeyService) {
        this.userApiKeyService = userApiKeyService;
    }

    @PostMapping
    public ResponseEntity<UserApiKeyCreateResponse> create(@Valid @RequestBody UserApiKeyCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userApiKeyService.create(request));
    }

    @GetMapping(params = "userId")
    public List<UserApiKeyResponse> findByUserId(@RequestParam Long userId) {
        return userApiKeyService.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public UserApiKeyResponse getById(@PathVariable Long id) {
        return userApiKeyService.getById(id);
    }

    @GetMapping("/{id}/detail")
    public UserApiKeyDetailResponse getDetailById(@PathVariable Long id) {
        return userApiKeyService.getDetailById(id);
    }

    @PutMapping("/{id}")
    public UserApiKeyResponse update(@PathVariable Long id, @Valid @RequestBody UserApiKeyUpdateRequest request) {
        return userApiKeyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userApiKeyService.delete(id);
    }
}
