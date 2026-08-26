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

import com.codingas.gateway.provider.model.ModelUpdateCommand;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型更新请求 DTO（HTTP 契约）
 */
@Data
public class ModelUpdateRequest {
    /** 供应商侧模型标识（如 gpt-4o），为 null 表示不更新 */
    @Size(max = 128, message = "Model name must not exceed 128 characters")
    private String modelName;

    @Size(max = 256, message = "Display name must not exceed 256 characters")
    private String displayName;

    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;

    /**
     * 转换为核心更新用例入参
     *
     * @return 更新用例入参
     */
    public ModelUpdateCommand toCommand() {
        return new ModelUpdateCommand(modelName, displayName, modelFamily, contextWindow,
                maxInputTokens, maxOutputTokens, capabilities, modalities);
    }
}
