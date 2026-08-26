/*
 * Copyright © 2025-2026 codingas.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingas.gateway.web.api;

import com.codingas.gateway.provider.vendor.ConnectivityTestResult;
import com.codingas.gateway.provider.vendor.ProviderManager;
import com.codingas.gateway.web.api.dto.*;
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

    private final ProviderManager providerManager;

    /**
     * 创建提供商
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderResponse create(@Valid @RequestBody ProviderCreateRequest request) {
        return ProviderResponse.from(providerManager.create(request.toCommand()));
    }

    /**
     * 获取提供商详情
     */
    @GetMapping("/{id}")
    public ProviderResponse getById(@PathVariable Long id) {
        return ProviderResponse.from(providerManager.getById(id));
    }

    /**
     * 查询提供商列表
     */
    @GetMapping
    public PageResponse<ProviderResponse> query(ProviderQueryRequest request) {
        return ProviderResponse.fromPage(providerManager.query(request.toQuery()));
    }

    /**
     * 更新提供商
     */
    @PutMapping("/{id}")
    public ProviderResponse update(@PathVariable Long id,
                                   @Valid @RequestBody ProviderUpdateRequest request) {
        return ProviderResponse.from(providerManager.update(id, request.toCommand()));
    }

    /**
     * 删除提供商
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        providerManager.delete(id);
    }

    /**
     * 获取所有供应商名称列表
     *
     * <p>返回所有已注册供应商的名称，供前端选择。</p>
     */
    @GetMapping("/names")
    public List<String> getProviderNames() {
        return providerManager.getProviderNames();
    }

    /**
     * 测试连通性
     */
    @PostMapping("/test-connectivity")
    public ConnectivityTestResult testConnectivity(
            @Valid @RequestBody ConnectivityTestRequest request) {
        return providerManager.testConnectivity(request.toCommand());
    }
}
