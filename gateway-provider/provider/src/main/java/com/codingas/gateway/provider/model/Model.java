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
package com.codingas.gateway.provider.model;

import com.codingas.gateway.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 模型规格实体（全局模型注册表）
 *
 * <p>Model 是模型固有规格，与渠道无关。modelName 是用户请求时传的值，路由匹配的唯一键。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Model extends BaseEntity {

    /** 用户面标识（如 "deepseek-v4-flash"，路由匹配用） */
    private String modelName;

    private String displayName;

    private String modelFamily;

    private Integer contextWindow;

    private Integer maxInputTokens;

    private Integer maxOutputTokens;

    /** 知识截止日期 */
    private String knowledgeCutoff;

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

    /** 人工锁定字段名集合（同步不覆盖） */
    private List<String> lockedFields;

    private Map<String, Boolean> capabilities;

    private List<String> modalities;

    /** 上游标记废弃时间，null 表示正常 */
    private Instant deprecatedAt;

    /** 计划下线日期 */
    private Instant scheduledRetiredAt;

    /** 下线原因或建议迁移目标 */
    private String deprecationMessage;

    

    /**
     * 检查模型是否可用（未被废弃）
     */
    public boolean isAvailable() {
        return deprecatedAt == null;
    }
}