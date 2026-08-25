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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.provider.vendor.Provider;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 提供商响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(Provider)} 从 {@code Provider} 实体生成
 * （providerId 取实体 code，沿用原契约字段语义）。</p>
 */
@Data
public class ProviderResponse {
    private Long id;
    private String providerId;
    private String providerName;
    private String description;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从提供商实体转换
     *
     * @param provider 提供商实体
     * @return 提供商响应 DTO
     */
    public static ProviderResponse from(Provider provider) {
        ProviderResponse response = new ProviderResponse();
        response.setId(provider.getId());
        response.setProviderId(provider.getCode());
        response.setProviderName(provider.getName());
        response.setWebsiteUrl(provider.getWebsiteUrl());
        response.setApiDocUrl(provider.getApiDocUrl());
        response.setPriority(provider.getPriority());
        response.setCreatedAt(provider.getCreatedAt());
        response.setUpdatedAt(provider.getUpdatedAt());
        return response;
    }

    /**
     * 从提供商实体列表转换
     *
     * @param providers 提供商实体列表
     * @return 提供商响应 DTO 列表
     */
    public static List<ProviderResponse> from(List<Provider> providers) {
        return providers.stream().map(ProviderResponse::from).toList();
    }

    /**
     * 从提供商实体分页转换
     *
     * @param page 提供商实体分页
     * @return 提供商响应 DTO 分页
     */
    public static PageResponse<ProviderResponse> fromPage(PageResponse<Provider> page) {
        return PageResponse.of(
                page.getItems().stream().map(ProviderResponse::from).toList(),
                page.getPagination().getPage(),
                page.getPagination().getLimit(),
                page.getPagination().getTotal());
    }
}
