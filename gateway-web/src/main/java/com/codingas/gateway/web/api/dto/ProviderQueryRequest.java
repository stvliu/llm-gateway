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

import com.codingas.gateway.common.dto.PageRequest;
import com.codingas.gateway.provider.vendor.ProviderQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提供商查询请求 DTO（HTTP 契约）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProviderQueryRequest extends PageRequest {
    private String keyword;

    /**
     * 转换为核心查询条件入参
     *
     * @return 查询条件
     */
    public ProviderQuery toQuery() {
        ProviderQuery query = new ProviderQuery();
        query.setPage(getPage());
        query.setLimit(getLimit());
        query.setKeyword(keyword);
        return query;
    }
}
