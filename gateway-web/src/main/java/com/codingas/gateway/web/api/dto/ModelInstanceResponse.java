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
 * 模型实例响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(ModelInstance)} 做基础字段纯映射；模型规格展示字段
 * （modelName/displayName/modelFamily）由 web 层组装器（Assembler）补充。</p>
 */
@Data
public class ModelInstanceResponse {
    private Long id;
    private Long channelId;
    private Long modelId;
    private String modelName;
    private String displayName;
    private String modelFamily;
    private String upstreamModelName;
    private Integer priority;
    private Integer weight;
    private String state;

    /**
     * 从模型实例实体纯映射基础字段
     *
     * @param instance 模型实例实体
     * @return 模型实例响应 DTO（modelName 等由组装器补充）
     */
    public static ModelInstanceResponse from(ModelInstance instance) {
        ModelInstanceResponse resp = new ModelInstanceResponse();
        resp.setId(instance.getId());
        resp.setChannelId(instance.getChannelId());
        resp.setModelId(instance.getModelId());
        resp.setUpstreamModelName(instance.getUpstreamModelName());
        resp.setPriority(instance.getPriority());
        resp.setWeight(instance.getWeight());
        resp.setState(instance.getState().name());
        return resp;
    }
}
