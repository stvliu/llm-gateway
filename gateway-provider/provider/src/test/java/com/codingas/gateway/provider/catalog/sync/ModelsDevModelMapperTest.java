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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelsDevModelMapper 单元测试
 *
 * <p>验证 models.dev 目录 DTO → Model 实体的映射：canonical ID 提取、
 * 能力/限额/元信息映射、限额回退规则与人工锁定字段保护。</p>
 */
class ModelsDevModelMapperTest {

    @Test
    @DisplayName("canonical ID 末段提取 modelName")
    void baseModelName_extractsLastSegment() {
        assertThat(ModelsDevModelMapper.baseModelName("openai/gpt-4o")).isEqualTo("gpt-4o");
        assertThat(ModelsDevModelMapper.baseModelName("deepseek-v4-flash")).isEqualTo("deepseek-v4-flash");
    }

    @Test
    @DisplayName("新模型完整映射：能力/限额/元信息")
    void toNewModel_mapsAllFields() {
        ModelCatalogDto dto = new ModelCatalogDto(
                "openai/gpt-4o", "GPT-4o", "Omnilingual multimodal", "gpt",
                true, false, true, true, true, "2023-10",
                "2024-05-13", "2026-08-01", false,
                new ModelCatalogDto.ModalitiesDto(List.of("text", "image"), List.of("text")),
                new ModelCatalogDto.LimitDto(128000L, 16384L, 128000L),
                "Proprietary",
                List.of(Map.of("name", "MMMU", "score", 69.1)),
                List.of(Map.of("label", "Model Card", "url", "https://example.com")));

        Model model = ModelsDevModelMapper.toNewModel(dto);

        assertThat(model.getModelName()).isEqualTo("gpt-4o");
        assertThat(model.getDisplayName()).isEqualTo("GPT-4o");
        assertThat(model.getDescription()).isEqualTo("Omnilingual multimodal");
        assertThat(model.getModelFamily()).isEqualTo("gpt");
        assertThat(model.getContextWindow()).isEqualTo(128000);
        assertThat(model.getMaxInputTokens()).isEqualTo(128000);
        assertThat(model.getMaxOutputTokens()).isEqualTo(16384);
        assertThat(model.getKnowledgeCutoff()).isEqualTo("2023-10");
        assertThat(model.getReleaseDate()).isEqualTo(LocalDate.of(2024, 5, 13));
        assertThat(model.getLastUpdated()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(model.getLicense()).isEqualTo("Proprietary");
        assertThat(model.getOpenWeights()).isFalse();
        assertThat(model.getCapabilities())
                .containsEntry("vision", true)
                .containsEntry("reasoning", false)
                .containsEntry("function_calling", true)
                .containsEntry("structured_output", true);
        assertThat(model.getModalities()).containsExactlyInAnyOrder("text", "image");
        assertThat(model.getSource()).isEqualTo("MODELS_DEV");
        assertThat(model.getExternalId()).isEqualTo("openai/gpt-4o");
        assertThat(model.getBenchmarks()).hasSize(1);
        assertThat(model.getWeights()).hasSize(1);
        assertThat(model.getLockedFields()).isNullOrEmpty();
        assertThat(model.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("limit.input 缺失时回退 contextWindow")
    void toNewModel_limitInputMissing_fallsBackToContext() {
        ModelCatalogDto dto = new ModelCatalogDto(
                "minimal/minimal-model", "Minimal", "desc", null,
                false, false, false, null, true, null,
                "2026-01-01", "2026-01-01", false,
                new ModelCatalogDto.ModalitiesDto(List.of("text"), List.of("text")),
                new ModelCatalogDto.LimitDto(8192L, null, null), null, null, null);

        Model model = ModelsDevModelMapper.toNewModel(dto);

        assertThat(model.getContextWindow()).isEqualTo(8192);
        assertThat(model.getMaxInputTokens()).isEqualTo(8192);
        assertThat(model.getMaxOutputTokens()).isNull();
        assertThat(model.getCapabilities()).containsEntry("vision", false);
        assertThat(model.getModalities()).containsExactly("text");
    }

    @Test
    @DisplayName("日期字段容错解析：YYYY-MM 补日、非法值置空")
    void toNewModel_dateFormats_parseLeniently() {
        ModelCatalogDto dto = new ModelCatalogDto(
                "openai/gpt-4o", "GPT-4o", "desc", "gpt",
                true, false, true, true, true, "2023-10",
                "2026-01", "invalid-date", false,
                new ModelCatalogDto.ModalitiesDto(List.of("text"), List.of("text")),
                new ModelCatalogDto.LimitDto(128000L, 16384L, 128000L),
                "Proprietary", List.of(), List.of());

        Model model = ModelsDevModelMapper.toNewModel(dto);

        // YYYY-MM 缺日 → 补 01
        assertThat(model.getReleaseDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        // 非法日期 → 置空不阻断
        assertThat(model.getLastUpdated()).isNull();
    }

    @Test
    @DisplayName("merge 跳过人工锁定字段")
    void merge_skipsLockedFields() {
        ModelCatalogDto dto = new ModelCatalogDto(
                "openai/gpt-4o", "GPT-4o new", "new desc", "gpt-new",
                true, true, true, true, true, "2024-01",
                "2025-01-01", "2026-08-01", false,
                new ModelCatalogDto.ModalitiesDto(List.of("text"), List.of("text")),
                new ModelCatalogDto.LimitDto(200000L, 100000L, 200000L),
                "MIT", List.of(), List.of());

        Model existing = new Model();
        existing.setModelName("gpt-4o");
        existing.setDisplayName("GPT-4o old");
        existing.setContextWindow(128000);
        existing.setLockedFields(List.of("displayName", "contextWindow"));

        ModelsDevModelMapper.merge(dto, existing);

        // 锁定字段不被覆盖
        assertThat(existing.getDisplayName()).isEqualTo("GPT-4o old");
        assertThat(existing.getContextWindow()).isEqualTo(128000);
        // 未锁定字段被更新
        assertThat(existing.getDescription()).isEqualTo("new desc");
        assertThat(existing.getModelFamily()).isEqualTo("gpt-new");
        assertThat(existing.getMaxInputTokens()).isEqualTo(200000);
        assertThat(existing.getMaxOutputTokens()).isEqualTo(100000);
        assertThat(existing.getKnowledgeCutoff()).isEqualTo("2024-01");
        assertThat(existing.getReleaseDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(existing.getLicense()).isEqualTo("MIT");
        assertThat(existing.getCapabilities()).containsEntry("reasoning", true);
        // externalId/source 由同步服务负责，merge 不修改 modelName
        assertThat(existing.getModelName()).isEqualTo("gpt-4o");
    }
}
