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

import com.codingas.gateway.provider.model.ModelInstanceUpdateCommand;
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
     * 转换为核心更新用例入参（channelId 仅用于归属校验）
     *
     * @return 更新用例入参
     */
    public ModelInstanceUpdateCommand toCommand() {
        return new ModelInstanceUpdateCommand(modelId, upstreamModelName);
    }
}
