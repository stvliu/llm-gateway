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

/**
 * 提供商更新用例入参
 *
 * <p>仅非 null 字段参与更新。</p>
 */
@Getter
@AllArgsConstructor
public class ProviderUpdateCommand {

    /** 提供商名称（可选） */
    private final String providerName;

    /** 官网 URL（可选） */
    private final String websiteUrl;

    /** API 文档 URL（可选） */
    private final String apiDocUrl;

    /** 优先级（可选） */
    private final Integer priority;
}
