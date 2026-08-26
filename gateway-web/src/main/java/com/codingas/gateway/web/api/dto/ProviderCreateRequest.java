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

import com.codingas.gateway.provider.vendor.ModelNestedCommand;
import com.codingas.gateway.provider.vendor.ProviderCreateCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 提供商创建请求 DTO（HTTP 契约）
 */
@Data
public class ProviderCreateRequest {
    /** 品牌标识（如 openai、anthropic），全局唯一 */
    @NotBlank(message = "品牌标识不能为空")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,62}[a-z0-9]$", message = "品牌标识只能包含小写字母、数字和中划线，长度3-64")
    private String code;

    @NotBlank(message = "Provider name is required")
    @Size(max = 128, message = "Provider name must not exceed 128 characters")
    private String providerName;

    @Size(max = 512, message = "Website URL must not exceed 512 characters")
    private String websiteUrl;

    @Size(max = 512, message = "API doc URL must not exceed 512 characters")
    private String apiDocUrl;

    private Integer priority = 100;

    @Valid
    private List<ModelNestedRequest> models;

    /**
     * 转换为核心创建用例入参
     *
     * @return 创建用例入参
     */
    public ProviderCreateCommand toCommand() {
        List<ModelNestedCommand> modelCommands = models != null
                ? models.stream().map(ModelNestedRequest::toCommand).toList()
                : null;
        return new ProviderCreateCommand(code, providerName, websiteUrl, apiDocUrl, priority, modelCommands);
    }
}
