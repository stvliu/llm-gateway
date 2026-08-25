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

/**
 * 模型实例更新用例入参
 *
 * <p>字段为 null 表示不更新该字段。</p>
 */
@Getter
@AllArgsConstructor
public class ModelInstanceUpdateCommand {

    /** 关联的模型规格 ID（可选） */
    private final Long modelId;

    /** 上游模型名（可选；null 表示不更新，空字符串表示清除） */
    private final String upstreamModelName;
}
