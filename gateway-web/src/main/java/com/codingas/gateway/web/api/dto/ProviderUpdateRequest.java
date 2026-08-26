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

import com.codingas.gateway.provider.vendor.ProviderUpdateCommand;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提供商更新请求 DTO（HTTP 契约）
 */
@Data
public class ProviderUpdateRequest {
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority;

    /**
     * 转换为核心更新用例入参
     *
     * @return 更新用例入参
     */
    public ProviderUpdateCommand toCommand() {
        return new ProviderUpdateCommand(providerName, websiteUrl, apiDocUrl, priority);
    }
}
