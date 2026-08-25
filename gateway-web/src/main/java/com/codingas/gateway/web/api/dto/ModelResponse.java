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

import com.codingas.gateway.common.dto.PageResponse;
import com.codingas.gateway.provider.model.Model;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 模型响应 DTO（HTTP 契约）
 *
 * <p>由 {@link #from(Model)} 从 {@code Model} 实体生成（状态字段由 isAvailable 推导）。</p>
 */
@Data
public class ModelResponse {
    private Long id;
    private String modelName;
    private String displayName;
    private String modelFamily;
    private Integer contextWindow;
    private Integer maxInputTokens;
    private Integer maxOutputTokens;
    private Map<String, Boolean> capabilities;
    private List<String> modalities;
    private Instant deprecatedAt;
    private String deprecationMessage;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * 从模型实体转换
     *
     * @param model 模型实体
     * @return 模型响应 DTO
     */
    public static ModelResponse from(Model model) {
        ModelResponse response = new ModelResponse();
        response.setId(model.getId());
        response.setModelName(model.getModelName());
        response.setDisplayName(model.getDisplayName());
        response.setModelFamily(model.getModelFamily());
        response.setContextWindow(model.getContextWindow());
        response.setMaxInputTokens(model.getMaxInputTokens());
        response.setMaxOutputTokens(model.getMaxOutputTokens());
        response.setCapabilities(model.getCapabilities());
        response.setModalities(model.getModalities());
        response.setDeprecatedAt(model.getDeprecatedAt());
        response.setDeprecationMessage(model.getDeprecationMessage());
        // 状态：未废弃为 ACTIVE，已废弃为 INACTIVE
        response.setState(model.isAvailable() ? "ACTIVE" : "INACTIVE");
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
        return response;
    }

    /**
     * 从模型实体列表转换
     *
     * @param models 模型实体列表
     * @return 模型响应 DTO 列表
     */
    public static List<ModelResponse> from(List<Model> models) {
        return models.stream().map(ModelResponse::from).toList();
    }

    /**
     * 从模型实体分页转换
     *
     * @param page 模型实体分页
     * @return 模型响应 DTO 分页
     */
    public static PageResponse<ModelResponse> fromPage(PageResponse<Model> page) {
        return PageResponse.of(
                page.getItems().stream().map(ModelResponse::from).toList(),
                page.getPagination().getPage(),
                page.getPagination().getLimit(),
                page.getPagination().getTotal());
    }
}
