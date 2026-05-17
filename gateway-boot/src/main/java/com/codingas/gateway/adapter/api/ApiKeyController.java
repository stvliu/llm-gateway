package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyCreateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyQueryRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyResponse;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUpdateRequest;
import com.codingas.gateway.application.gatewayapikey.dto.ApiKeyUsageResponse;
import com.codingas.gateway.application.gatewayapikey.ApiKeyService;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

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
     * 查询 API Key 列表
     */
    @GetMapping
    public PageResponse<ApiKeyResponse> query(@ModelAttribute ApiKeyQueryRequest request) {
        return apiKeyService.query(request);
    }

    /**
     * 批量获取 API Key 用量统计
     */
    @GetMapping(path = "/stats")
    public List<ApiKeyUsageResponse> getUsageBatch(
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) Long userId) {
        return apiKeyService.getUsageBatch(startDate, endDate, userId);
    }

    /**
     * 获取 API Key 详情
     */
    @GetMapping("/{id}")
    public ApiKeyResponse getById(@PathVariable Long id) {
        return apiKeyService.getById(id);
    }

    /**
     * 获取单个 API Key 用量统计
     */
    @GetMapping("/{id}/stats")
    public ApiKeyUsageResponse getUsage(
            @PathVariable Long id,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {
        return apiKeyService.getUsage(id, startDate, endDate);
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
