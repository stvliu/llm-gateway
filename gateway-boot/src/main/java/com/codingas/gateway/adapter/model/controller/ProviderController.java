package com.codingas.gateway.adapter.model.controller;

import com.codingas.gateway.adapter.model.dto.CreateProviderRequest;
import com.codingas.gateway.adapter.model.dto.ProviderResponse;
import com.codingas.gateway.adapter.model.dto.UpdateProviderRequest;
import com.codingas.gateway.application.model.ProviderManageUseCase;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.domain.router.entity.Provider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供商管理 API
 *
 * <p>处理 Provider 的 CRUD 操作。</p>
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderManageUseCase providerManageUseCase;

    @GetMapping
    public ApiResponse<List<ProviderResponse>> list() {
        List<Provider> providers = providerManageUseCase.findAll();
        List<ProviderResponse> response = providers.stream()
                .map(ProviderResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProviderResponse> getById(@PathVariable Long id) {
        return providerManageUseCase.findById(id)
                .map(p -> ApiResponse.success(ProviderResponse.from(p)))
                .orElse(ApiResponse.error("NOT_FOUND", "Provider not found: " + id));
    }

    @PostMapping
    public ApiResponse<ProviderResponse> create(@Valid @RequestBody CreateProviderRequest request) {
        Provider provider = request.toEntity();
        Provider created = providerManageUseCase.create(provider);
        return ApiResponse.success(ProviderResponse.from(created));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProviderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderRequest request) {
        Provider provider = request.toEntity();
        Provider updated = providerManageUseCase.update(id, provider);
        return ApiResponse.success(ProviderResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerManageUseCase.delete(id);
        return ApiResponse.success();
    }
}
