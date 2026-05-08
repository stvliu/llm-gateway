package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.providerapikey.ProviderApiKeyService;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyCreateResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyQueryRequest;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyResponse;
import com.codingas.gateway.application.providerapikey.dto.ProviderApiKeyUpdateRequest;
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
    public ProviderApiKeyCreateResponse create(@Valid @RequestBody ProviderApiKeyCreateRequest request) {
        return providerApiKeyService.create(request);
    }

    @GetMapping("/{id}")
    public ProviderApiKeyResponse getById(@PathVariable Long id) {
        return providerApiKeyService.getById(id);
    }

    @GetMapping
    public PageResponse<ProviderApiKeyResponse> query(@ModelAttribute ProviderApiKeyQueryRequest request) {
        return providerApiKeyService.query(request);
    }

    @PutMapping("/{id}")
    public ProviderApiKeyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderApiKeyUpdateRequest request) {
        return providerApiKeyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        providerApiKeyService.delete(id);
    }

    @PatchMapping("/{id}/enabled")
    public ProviderApiKeyResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return providerApiKeyService.setEnabled(id, enabled);
    }
}