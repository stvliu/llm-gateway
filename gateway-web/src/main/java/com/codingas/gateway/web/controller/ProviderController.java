package com.codingas.gateway.web.controller;

import com.codingas.gateway.core.domain.entity.Provider;
import com.codingas.gateway.core.domain.enums.ProviderStatus;
import com.codingas.gateway.core.service.ProviderService;
import com.codingas.gateway.web.dto.ApiResponse;
import com.codingas.gateway.web.dto.CreateProviderRequest;
import com.codingas.gateway.web.dto.ProviderResponse;
import com.codingas.gateway.web.dto.UpdateProviderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Provider Management", description = "提供商管理接口")
public class ProviderController {

    private final ProviderService providerService;

    @GetMapping
    @Operation(summary = "获取提供商列表")
    public ApiResponse<List<ProviderResponse>> list(
            @RequestParam(required = false) ProviderStatus status) {
        List<Provider> providers;
        if (status != null) {
            providers = providerService.findByStatus(status);
        } else {
            providers = providerService.findAll();
        }
        List<ProviderResponse> response = providers.stream()
                .map(ProviderResponse::from)
                .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取提供商详情")
    public ApiResponse<ProviderResponse> getById(@PathVariable Long id) {
        return providerService.findById(id)
                .map(p -> ApiResponse.success(ProviderResponse.from(p)))
                .orElse(ApiResponse.error("NOT_FOUND", "Provider not found: " + id));
    }

    @PostMapping
    @Operation(summary = "创建提供商")
    public ApiResponse<ProviderResponse> create(@Valid @RequestBody CreateProviderRequest request) {
        Provider provider = request.toEntity();
        Provider created = providerService.create(provider);
        return ApiResponse.success(ProviderResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新提供商")
    public ApiResponse<ProviderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderRequest request) {
        Provider provider = request.toEntity();
        Provider updated = providerService.update(id, provider);
        return ApiResponse.success(ProviderResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除提供商")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/code/{providerCode}")
    @Operation(summary = "根据编码获取提供商")
    public ApiResponse<ProviderResponse> getByCode(@PathVariable String providerCode) {
        return providerService.findByProviderCode(providerCode)
                .map(p -> ApiResponse.success(ProviderResponse.from(p)))
                .orElse(ApiResponse.error("NOT_FOUND", "Provider not found: " + providerCode));
    }
}