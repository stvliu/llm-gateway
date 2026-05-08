package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.gatewayapikey.ApiKeyService;
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
    public ApiKeyResponse create(@Valid @RequestBody ApiKeyCreateRequest request) {
        return apiKeyService.create(request);
    }

    /**
     * 获取 API Key 详情
     */
    @GetMapping("/{id}")
    public ApiKeyResponse getById(@PathVariable Long id) {
        return apiKeyService.getById(id);
    }

    /**
     * 查询 API Key 列表
     */
    @GetMapping
    public PageResponse<ApiKeyResponse> query(@ModelAttribute ApiKeyQueryRequest request) {
        return apiKeyService.query(request);
    }

    /**
     * 更新 API Key
     */
    @PutMapping("/{id}")
    public ApiKeyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ApiKeyUpdateRequest request) {
        return apiKeyService.update(id, request);
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        apiKeyService.delete(id);
    }

    /**
     * 启用/禁用 API Key
     */
    @PatchMapping("/{id}/enabled")
    public ApiKeyResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return apiKeyService.setEnabled(id, enabled);
    }
}
