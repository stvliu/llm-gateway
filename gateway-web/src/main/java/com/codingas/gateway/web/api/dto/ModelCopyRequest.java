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

import com.codingas.gateway.provider.model.Model;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型复制请求 DTO（HTTP 契约）
 *
 * <p>仅承载可覆盖字段；未覆盖的规格字段从源模型继承。</p>
 */
@Data
public class ModelCopyRequest {

    /** 新模型名（必填，路由匹配唯一键） */
    @NotBlank(message = "modelName 不能为空")
    private String modelName;

    /** 显示名称（可选覆盖，null 继承源） */
    private String displayName;

    /** 模型族（可选覆盖，null 继承源） */
    private String modelFamily;

    /**
     * 转换为覆盖字段实体（其余规格由服务层从源模型复制）
     *
     * @return 仅含覆盖字段的 Model
     */
    public Model toEntity() {
        Model model = new Model();
        model.setModelName(modelName);
        model.setDisplayName(displayName);
        model.setModelFamily(modelFamily);
        return model;
    }
}
