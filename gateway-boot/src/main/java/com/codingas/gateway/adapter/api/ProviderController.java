package com.codingas.gateway.adapter.api;

import com.codingas.gateway.application.provider.ProviderService;
import com.codingas.gateway.application.provider.dto.ConnectivityTestRequest;
import com.codingas.gateway.application.provider.dto.ConnectivityTestResult;
import com.codingas.gateway.application.provider.dto.ProviderCreateRequest;
import com.codingas.gateway.application.provider.dto.ProviderQueryRequest;
import com.codingas.gateway.application.provider.dto.ProviderResponse;
import com.codingas.gateway.application.provider.dto.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供商管理 API
 *
 * <p>注意：API Key 管理已迁移到 ProductApiKey，通过 ProductController 管理。</p>
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
    public ResponseEntity<ProviderResponse> create(@Valid @RequestBody ProviderCreateRequest request) {
        return ResponseEntity.ok(providerService.create(request));
    }

    /**
     * 获取提供商详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getById(id));
    }

    /**
     * 查询提供商列表
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProviderResponse>> query(ProviderQueryRequest request) {
        return ResponseEntity.ok(providerService.query(request));
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody ProviderUpdateRequest request) {
        return ResponseEntity.ok(providerService.update(id, request));
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        providerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 启用/禁用提供商
     */
    @PatchMapping("/{id}/state")
    public ResponseEntity<ProviderResponse> setEnabled(@PathVariable Long id,
                                                        @RequestParam boolean enabled) {
        return ResponseEntity.ok(providerService.setEnabled(id, enabled));
    }

    /**
     * 获取所有供应商名称列表
     *
     * <p>返回所有已注册供应商的名称，供前端选择。</p>
     */
    @GetMapping("/names")
    public ResponseEntity<List<String>> getProviderNames() {
        return ResponseEntity.ok(providerService.getProviderNames());
    }

    /**
     * 测试连通性
     */
    @PostMapping("/test-connectivity")
    public ResponseEntity<ConnectivityTestResult> testConnectivity(
            @Valid @RequestBody ConnectivityTestRequest request) {
        return ResponseEntity.ok(providerService.testConnectivity(request));
    }
}