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

import com.codingas.gateway.provider.model.ModelCreateCommand;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模型创建请求 DTO（HTTP 契约）
 */
@Data
public class ModelCreateRequest {
    private String modelName;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;

    /**
     * 转换为核心创建用例入参
     *
     * @return 创建用例入参
     */
    public ModelCreateCommand toCommand() {
        return new ModelCreateCommand(modelName, displayName, modelFamily, contextWindow,
                maxInputTokens, maxOutputTokens, capabilities, modalities);
    }
}
