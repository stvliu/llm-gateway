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
import java.time.LocalDate;
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

    /** 模型描述（来自数据源） */
    private String description;

    /** 发布日期 */
    private LocalDate releaseDate;

    /** 数据源最后更新日期 */
    private LocalDate lastUpdated;

    /** 许可证（如 MIT） */
    private String license;

    /** 是否开源权重 */
    private Boolean openWeights;

    /** 基准测试分数 [{name, score, metric, source}] */
    private List<Map<String, Object>> benchmarks;

    /** 权重/模型卡片链接 [{label, url}] */
    private List<Map<String, Object>> weights;

    /** 数据来源：MODELS_DEV / BUILTIN / MANUAL */
    private String source;

    /** 数据源外部 ID（如 openai/gpt-4o），同步幂等匹配键 */
    private String externalId;

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
        // 透出同步后的模型元信息（models.dev 同步写入）
        response.setDescription(model.getDescription());
        response.setReleaseDate(model.getReleaseDate());
        response.setLastUpdated(model.getLastUpdated());
        response.setLicense(model.getLicense());
        response.setOpenWeights(model.getOpenWeights());
        response.setBenchmarks(model.getBenchmarks());
        response.setWeights(model.getWeights());
        response.setSource(model.getSource());
        response.setExternalId(model.getExternalId());
        return response;
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
