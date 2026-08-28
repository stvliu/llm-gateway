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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.provider.model.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * models.dev 模型目录 → Model 实体映射器
 *
 * <p>纯静态工具：canonical ID 提取 modelName、能力/限额/元信息字段映射、
 * 人工锁定字段保护（merge 跳过 lockedFields）。</p>
 */
public final class ModelsDevModelMapper {

    private ModelsDevModelMapper() {}

    /** models.dev 数据源标识 */
    public static final String SOURCE = "MODELS_DEV";

    /**
     * 提取 canonical ID 末段作为通用模型名
     */
    public static String baseModelName(String externalId) {
        int idx = externalId.lastIndexOf('/');
        return idx >= 0 ? externalId.substring(idx + 1) : externalId;
    }

    /**
     * 由目录 DTO 新建 Model 实体
     */
    public static Model toNewModel(ModelCatalogDto dto) {
        Model model = new Model();
        model.setModelName(baseModelName(dto.id()));
        model.setDisplayName(dto.name());
        model.setDescription(dto.description());
        model.setModelFamily(dto.family());
        model.setSource(SOURCE);
        model.setExternalId(dto.id());
        model.setReleaseDate(dto.releaseDate());
        model.setLastUpdated(dto.lastUpdated());
        model.setLicense(dto.license());
        model.setOpenWeights(dto.openWeights());
        model.setBenchmarks(dto.benchmarks());
        model.setWeights(dto.weights());
        applyLimits(dto, model);
        model.setKnowledgeCutoff(dto.knowledge());
        model.setCapabilities(toCapabilities(dto));
        model.setModalities(toModalities(dto));
        return model;
    }

    /**
     * 合并目录 DTO 到已有模型，跳过人工锁定字段
     */
    public static void merge(ModelCatalogDto dto, Model existing) {
        List<String> locked = existing.getLockedFields() == null
                ? List.of() : existing.getLockedFields();
        if (!locked.contains("displayName")) existing.setDisplayName(dto.name());
        if (!locked.contains("description")) existing.setDescription(dto.description());
        if (!locked.contains("modelFamily")) existing.setModelFamily(dto.family());
        if (!locked.contains("knowledgeCutoff")) existing.setKnowledgeCutoff(dto.knowledge());
        if (!locked.contains("contextWindow")) {
            existing.setContextWindow(intValue(dto.limit() == null ? null : dto.limit().context()));
        }
        if (!locked.contains("maxInputTokens")) {
            existing.setMaxInputTokens(resolveInputLimit(dto));
        }
        if (!locked.contains("maxOutputTokens")) {
            existing.setMaxOutputTokens(intValue(dto.limit() == null ? null : dto.limit().output()));
        }
        if (!locked.contains("capabilities")) existing.setCapabilities(toCapabilities(dto));
        if (!locked.contains("modalities")) existing.setModalities(toModalities(dto));
        if (!locked.contains("releaseDate")) existing.setReleaseDate(dto.releaseDate());
        if (!locked.contains("lastUpdated")) existing.setLastUpdated(dto.lastUpdated());
        if (!locked.contains("license")) existing.setLicense(dto.license());
        if (!locked.contains("openWeights")) existing.setOpenWeights(dto.openWeights());
        if (!locked.contains("benchmarks")) existing.setBenchmarks(dto.benchmarks());
        if (!locked.contains("weights")) existing.setWeights(dto.weights());
    }

    private static void applyLimits(ModelCatalogDto dto, Model model) {
        if (dto.limit() == null) return;
        model.setContextWindow(intValue(dto.limit().context()));
        model.setMaxInputTokens(resolveInputLimit(dto));
        model.setMaxOutputTokens(intValue(dto.limit().output()));
    }

    /** limit.input 缺失时回退 context */
    private static Integer resolveInputLimit(ModelCatalogDto dto) {
        if (dto.limit() == null) return null;
        return dto.limit().input() != null ? intValue(dto.limit().input())
                : intValue(dto.limit().context());
    }

    private static Integer intValue(Long value) {
        return value == null ? null : value.intValue();
    }

    /** 能力映射：attachment→vision、reasoning、tool_call→function_calling、structured_output */
    private static Map<String, Boolean> toCapabilities(ModelCatalogDto dto) {
        Map<String, Boolean> caps = new LinkedHashMap<>();
        caps.put("vision", Boolean.TRUE.equals(dto.attachment()));
        caps.put("reasoning", Boolean.TRUE.equals(dto.reasoning()));
        caps.put("function_calling", Boolean.TRUE.equals(dto.toolCall()));
        caps.put("structured_output", Boolean.TRUE.equals(dto.structuredOutput()));
        return caps;
    }

    /** 模态：输入+输出合并去重 */
    private static List<String> toModalities(ModelCatalogDto dto) {
        if (dto.modalities() == null) return List.of();
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (dto.modalities().input() != null) merged.addAll(dto.modalities().input());
        if (dto.modalities().output() != null) merged.addAll(dto.modalities().output());
        return new ArrayList<>(merged);
    }
}
