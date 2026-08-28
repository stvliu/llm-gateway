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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * models.dev 模型目录 DTO
 *
 * <p>对应 https://models.dev/models.json 中单个模型的 JSON 结构，
 * JSON 下划线字段通过 {@link JsonProperty} 映射为 camelCase 组件名。</p>
 */
public record ModelCatalogDto(
        String id, String name, String description, String family,
        Boolean attachment, Boolean reasoning,
        @JsonProperty("tool_call") Boolean toolCall,
        @JsonProperty("structured_output") Boolean structuredOutput,
        Boolean temperature, String knowledge,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("last_updated") String lastUpdated,
        @JsonProperty("open_weights") Boolean openWeights,
        ModalitiesDto modalities, LimitDto limit,
        String license,
        List<Map<String, Object>> benchmarks,
        List<Map<String, Object>> weights
) {
    /**
     * 模型输入/输出模态
     */
    public record ModalitiesDto(List<String> input, List<String> output) {}

    /**
     * 模型上下文/输出/输入 Token 限额
     */
    public record LimitDto(Long context, Long output, Long input) {}
}
