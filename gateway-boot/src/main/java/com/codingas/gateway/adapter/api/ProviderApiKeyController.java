package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.providerapikey.ProviderApiKeyService;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyQueryRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyUpdateRequest;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Provider API Key 管理控制器
 */
@RestController
@RequestMapping("/api/v1/provider-api-keys")
@RequiredArgsConstructor
public class ProviderApiKeyController {

    private final ProviderApiKeyService providerApiKeyService;

    @PostMapping
    public ApiResponse<ProviderApiKeyCreateResponse> create(@Valid @RequestBody ProviderApiKeyCreateRequest request) {
        return ApiResponse.success(providerApiKeyService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProviderApiKeyResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(providerApiKeyService.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProviderApiKeyResponse>> query(@ModelAttribute ProviderApiKeyQueryRequest request) {
        return ApiResponse.success(providerApiKeyService.query(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProviderApiKeyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderApiKeyUpdateRequest request) {
        return ApiResponse.success(providerApiKeyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerApiKeyService.delete(id);
        return ApiResponse.success();
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<ProviderApiKeyResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.success(providerApiKeyService.setEnabled(id, enabled));
    }
}