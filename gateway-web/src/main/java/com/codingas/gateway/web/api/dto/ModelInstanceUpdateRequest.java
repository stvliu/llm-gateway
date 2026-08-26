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

import com.codingas.gateway.provider.model.ModelInstance;
import lombok.Data;

/**
 * 模型实例更新请求 DTO（HTTP 契约）
 */
@Data
public class ModelInstanceUpdateRequest {
    private Long channelId;
    private Long modelId;
    private String upstreamModelName;

    /**
     * 转换为模型实例实体（null 字段表示不更新）
     *
     * @return 模型实例实体
     */
    public ModelInstance toEntity() {
        ModelInstance instance = new ModelInstance();
        instance.setModelId(modelId);
        instance.setUpstreamModelName(upstreamModelName);
        return instance;
    }
}
