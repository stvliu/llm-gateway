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
package com.codingas.gateway.provider.vendor;

import com.codingas.gateway.provider.model.Model;

import com.codingas.gateway.common.dto.PageResponse;

import java.util.List;

/**
 * 提供商管理服务接口
 *
 * <p>出入参采用实体与轻量用例对象，HTTP 契约（Request/Response DTO）由 web 层负责转换。</p>
 */
public interface ProviderService {

    /**
     * 创建提供商
     *
     * @param provider 提供商实体（承载 code/name/websiteUrl/apiDocUrl/priority）
     * @param models   嵌套模型列表（可选）
     * @return 创建后的提供商实体
     */
    Provider create(Provider provider, List<Model> models);

    /**
     * 根据 ID 获取提供商
     *
     * @param id 提供商 ID
     * @return 提供商实体
     */
    Provider getById(Long id);

    /**
     * 查询提供商列表
     *
     * @param query 查询条件
     * @return 提供商实体分页
     */
    PageResponse<Provider> query(ProviderQuery query);

    /**
     * 更新提供商（实体 null 字段表示不更新）
     *
     * @param id       提供商 ID
     * @param provider 提供商实体
     * @return 更新后的提供商实体
     */
    Provider update(Long id, Provider provider);

    /**
     * 删除提供商（软删除）
     *
     * @param id 提供商 ID
     */
    void delete(Long id);

    /**
     * 获取所有供应商名称列表
     *
     * @return 供应商名称列表
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
     * @param protocolName 协议名称（如 openai、anthropic）
     * @param baseUrl      供应商 Base URL（可选）
     * @param apiKey       待测试的 API Key
     * @param model        指定测试模型（可选）
     * @return 测试用例结果
     */
    ConnectivityTestResult testConnectivity(String protocolName, String baseUrl, String apiKey, String model);
}
