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
package com.codingas.gateway.provider.service;

import com.codingas.gateway.provider.vendor.ConnectivityTestRequest;
import com.codingas.gateway.provider.vendor.ConnectivityTestResult;
import com.codingas.gateway.provider.vendor.ProviderCreateRequest;
import com.codingas.gateway.provider.vendor.ProviderQueryRequest;
import com.codingas.gateway.provider.vendor.ProviderResponse;
import com.codingas.gateway.provider.vendor.ProviderUpdateRequest;
import com.codingas.gateway.common.dto.PageResponse;

import java.util.List;

/**
 * 提供商应用服务接口
 *
 * <p>处理提供商管理的业务逻辑。</p>
 */
public interface ProviderService {

    /**
     * 创建提供商
     */
    ProviderResponse create(ProviderCreateRequest request);

    /**
     * 根据 ID 获取提供商
     */
    ProviderResponse getById(Long id);

    /**
     * 查询提供商列表
     */
    PageResponse<ProviderResponse> query(ProviderQueryRequest request);

    /**
     * 更新提供商
     */
    ProviderResponse update(Long id, ProviderUpdateRequest request);

    /**
     * 删除提供商（软删除）
     */
    void delete(Long id);

    /**
     * 获取所有供应商名称列表
     */
    List<String> getProviderNames();

    /**
     * 测试连通性
     *
     * <p>执行分层连通性测试：</p>
     * <ul>
     *   <li>Level 1：认证验证（获取模型列表或最小请求）</li>
     *   <li>Level 2：模型可用性验证（发送最小 chat 请求）</li>
     * </ul>
     *
     * @param request 测试请求
     * @return 测试结果
     */
    ConnectivityTestResult testConnectivity(ConnectivityTestRequest request);
}