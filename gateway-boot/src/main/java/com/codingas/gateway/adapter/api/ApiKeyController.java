package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.apikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.apikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.apikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.apikey.ApiKeyService;
import com.codingas.gateway.common.dto.ApiResponse;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API Key 管理控制器
 *
 * <p>提供 API Key CRUD 操作的 REST API 端点。</p>
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * 创建 API Key
     */
    @PostMapping
    public ApiResponse<ApiKeyResponse> create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return ApiResponse.success(apiKeyService.create(request));
    }

    /**
     * 获取 API Key 详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ApiKeyResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(apiKeyService.getById(id));
    }

    /**
     * 查询 API Key 列表
     */
    @GetMapping
    public ApiResponse<PageResponse<ApiKeyResponse>> query(@ModelAttribute ApiKeyQueryRequest request) {
        return ApiResponse.success(apiKeyService.query(request));
    }

    /**
     * 更新 API Key
     */
    @PutMapping("/{id}")
    public ApiResponse<ApiKeyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyUpdateRequest request) {
        return ApiResponse.success(apiKeyService.update(id, request));
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        apiKeyService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 启用/禁用 API Key
     */
    @PatchMapping("/{id}/enabled")
    public ApiResponse<ApiKeyResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ApiResponse.success(apiKeyService.setEnabled(id, enabled));
    }
}
