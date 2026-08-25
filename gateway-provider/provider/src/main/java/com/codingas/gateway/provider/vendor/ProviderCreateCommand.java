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
package com.codingas.gateway.provider.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 提供商创建用例入参
 *
 * <p>字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.ProviderCreateRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class ProviderCreateCommand {

    /** 提供商编码 */
    private final String code;

    /** 提供商名称 */
    private final String providerName;

    /** 官网 URL */
    private final String websiteUrl;

    /** API 文档 URL */
    private final String apiDocUrl;

    /** 优先级（null 时核心默认 100） */
    private final Integer priority;

    /** 嵌套模型列表（可选） */
    private final List<ModelNestedCommand> models;
}
