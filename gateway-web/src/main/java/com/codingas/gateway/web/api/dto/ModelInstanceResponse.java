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
import com.codingas.gateway.provider.model.ModelInstanceView;
import lombok.Data;

import java.util.List;

/**
 * 模型实例响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(ModelInstanceView)} 从核心组装好的模型实例视图对象纯映射生成
 * （模型规格展示字段已由核心 Service 组装，转换层不依赖持久化仓储）。</p>
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
     * 从模型实例视图对象纯映射转换
     *
     * @param view 模型实例视图对象（含模型实例实体与模型规格关联）
     * @return 模型实例响应 DTO
     */
    public static ModelInstanceResponse from(ModelInstanceView view) {
        ModelInstance instance = view.getInstance();
        ModelInstanceResponse resp = new ModelInstanceResponse();
        resp.setId(instance.getId());
        resp.setChannelId(instance.getChannelId());
        resp.setModelId(instance.getModelId());
        resp.setUpstreamModelName(instance.getUpstreamModelName());
        resp.setPriority(instance.getPriority());
        resp.setWeight(instance.getWeight());
        resp.setState(instance.getState().name());

        if (view.getModel() != null) {
            resp.setModelName(view.getModel().getModelName());
            resp.setDisplayName(view.getModel().getDisplayName());
            resp.setModelFamily(view.getModel().getModelFamily());
        }

        return resp;
    }

    /**
     * 从模型实例视图对象列表纯映射转换
     *
     * @param views 模型实例视图对象列表
     * @return 模型实例响应 DTO 列表
     */
    public static List<ModelInstanceResponse> from(List<ModelInstanceView> views) {
        return views.stream().map(ModelInstanceResponse::from).toList();
    }
}
