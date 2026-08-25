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
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 提供商下嵌套模型请求 DTO（HTTP 契约）
 */
@Data
public class ModelNestedRequest {
    private String modelName;
    private String displayName;
    private Integer contextWindow;
    private BigDecimal inputPrice;
    private BigDecimal outputPrice;
    private Map<String, Boolean> capabilities;

    /**
     * 转换为核心嵌套模型用例入参
     *
     * @return 嵌套模型用例入参
     */
    public ModelNestedCommand toCommand() {
        return new ModelNestedCommand(modelName, displayName, contextWindow,
                inputPrice, outputPrice, capabilities);
    }
}
