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

import java.util.List;

/**
 * 体验模型响应 DTO（HTTP 契约）
 *
 * <p>用于模型体验功能，返回简化的模型信息。</p>
 *
 * @param id          模型 ID
 * @param modelName   模型名称
 * @param displayName 展示名称（为空时回退 modelName）
 */
public record ExperienceModelResponse(
    Long id,
    String modelName,
    String displayName
) {
    /**
     * 从模型实体转换
     *
     * @param model 模型实体
     * @return 体验模型响应 DTO
     */
    public static ExperienceModelResponse from(Model model) {
        return new ExperienceModelResponse(
                model.getId(),
                model.getModelName(),
                model.getDisplayName() != null ? model.getDisplayName() : model.getModelName());
    }

    /**
     * 从模型实体列表转换
     *
     * @param models 模型实体列表
     * @return 体验模型响应 DTO 列表
     */
    public static List<ExperienceModelResponse> from(List<Model> models) {
        return models.stream().map(ExperienceModelResponse::from).toList();
    }
}
