package com.codingas.gateway.adapter.admin.controller;

import com.codingas.gateway.adapter.admin.dto.provider.ProviderCreateRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderQueryRequest;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderResponse;
import com.codingas.gateway.adapter.admin.dto.provider.ProviderUpdateRequest;
import com.codingas.gateway.application.provider.ProviderApplication;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 提供商管理控制器
 *
 * <p>提供提供商 CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderApplication providerApplication;

    /**
     * 创建提供商
     */
    @PostMapping
    public ApiResponse<ProviderResponse> create(@Valid @RequestBody ProviderCreateRequest request) {
        return ApiResponse.success(providerApplication.create(request));
    }

    /**
     * 获取提供商详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ProviderResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(providerApplication.getById(id));
    }

    /**
     * 查询提供商列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ProviderResponse>> query(@ModelAttribute ProviderQueryRequest request) {
        return ApiResponse.success(providerApplication.query(request));
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ApiResponse<ProviderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderUpdateRequest request) {
        return ApiResponse.success(providerApplication.update(id, request));
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        providerApplication.delete(id);
        return ApiResponse.success();
    }

    /**
     * 启用/禁用提供商
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<ProviderResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.success(providerApplication.setEnabled(id, enabled));
    }
}
