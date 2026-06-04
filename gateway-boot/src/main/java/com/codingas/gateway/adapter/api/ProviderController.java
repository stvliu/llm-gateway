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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供商管理 API
 *
 * <p>注意：API Key 管理已迁移到 ChannelCredential，通过 ChannelCredentialController 管理。</p>
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
    @ResponseStatus(HttpStatus.CREATED)
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
    public PageResponse<ProviderResponse> query(ProviderQueryRequest request) {
        return providerService.query(request);
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ProviderResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ProviderUpdateRequest request) {
        return providerService.update(id, request);
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        providerService.delete(id);
    }

    /**
     * 启用/禁用提供商
     */
    @PatchMapping("/{id}/state")
    public ProviderResponse setEnabled(@PathVariable Long id,
                                       @RequestParam boolean enabled) {
        return providerService.setEnabled(id, enabled);
    }

    /**
     * 获取所有供应商名称列表
     *
     * <p>返回所有已注册供应商的名称，供前端选择。</p>
     */
    @GetMapping("/names")
    public List<String> getProviderNames() {
        return providerService.getProviderNames();
    }

    /**
     * 测试连通性
     */
    @PostMapping("/test-connectivity")
    public ConnectivityTestResult testConnectivity(
            @Valid @RequestBody ConnectivityTestRequest request) {
        return providerService.testConnectivity(request);
    }
}
