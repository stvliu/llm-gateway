package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderKeysResponse;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.application.provider.ProviderService;
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

    private final ProviderService providerService;

    /**
     * 创建提供商
     */
    @PostMapping
    public ProviderResponse create(@Valid @RequestBody ProviderCreateRequest request) {
        return providerService.create(request);
    }

    /**
     * 获取提供商详情
     */
    @GetMapping("/{id}")
    public ProviderResponse getById(@PathVariable Long id) {
        return providerService.getById(id);
    }

    /**
     * 查询提供商列表
     */
    @GetMapping
    public PageResponse<ProviderResponse> query(@ModelAttribute ProviderQueryRequest request) {
        return providerService.query(request);
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ProviderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ProviderUpdateRequest request) {
        return providerService.update(id, request);
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        providerService.delete(id);
    }

    /**
     * 启用/禁用提供商
     */
    @PatchMapping("/{id}/enabled")
    public ProviderResponse setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return providerService.setEnabled(id, enabled);
    }

    /**
     * 获取 Provider 的 Key 信息
     */
    @GetMapping("/{id}/keys")
    public ProviderKeysResponse getProviderKeys(@PathVariable Long id) {
        return providerService.getProviderKeys(id);
    }
}