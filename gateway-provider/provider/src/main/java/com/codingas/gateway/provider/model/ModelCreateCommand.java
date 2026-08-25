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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 模型创建用例入参
 *
 * <p>字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.ModelCreateRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class ModelCreateCommand {

    /** 模型名称 */
    private final String modelName;

    /** 展示名称 */
    private final String displayName;

    /** 模型系列 */
    private final String modelFamily;

    /** 上下文窗口 */
    private final Integer contextWindow;

    /** 最大输入 Token 数 */
    private final Integer maxInputTokens;

    /** 最大输出 Token 数 */
    private final Integer maxOutputTokens;

    /** 能力覆盖配置 */
    private final Map<String, Boolean> capabilities;

    /** 模态（文本/图像等） */
    private final List<String> modalities;
}
