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
package com.codingas.gateway.providerdata.model;

import com.codingas.gateway.common.data.BaseDo;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * 模型数据对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "models")
public class ModelDo extends BaseDo {


    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "model_family", length = 64)
    private String modelFamily;

    @Column(name = "context_window")
    private Integer contextWindow;

    @Column(name = "max_input_tokens")
    private Integer maxInputTokens;

    @Column(name = "max_output_tokens")
    private Integer maxOutputTokens;

    /** 知识截止日期 */
    @Column(name = "knowledge_cutoff", length = 32)
    private String knowledgeCutoff;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @Column(name = "license", length = 128)
    private String license;

    @Column(name = "open_weights")
    private Boolean openWeights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "benchmarks", columnDefinition = "jsonb")
    private List<Map<String, Object>> benchmarks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weights", columnDefinition = "jsonb")
    private List<Map<String, Object>> weights;

    @Column(name = "source", length = 32)
    private String source;

    @Column(name = "external_id", length = 256)
    private String externalId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "locked_fields", columnDefinition = "jsonb")
    private List<String> lockedFields;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private Map<String, Boolean> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "modalities", columnDefinition = "jsonb")
    private List<String> modalities;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;

    @Column(name = "scheduled_retired_at")
    private Instant scheduledRetiredAt;

    @Column(name = "deprecation_message", length = 512)
    private String deprecationMessage;
}