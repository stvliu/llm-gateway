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
package com.codingas.gateway.provider.channel;

/**
 * 内联供应商参数
 *
 * @param code        供应商程序标识，必须与 planCode 解析出的 providerCode 一致
 * @param name        显示名（缺省时回退为 code）
 * @param description 描述
 * @param websiteUrl  官网 URL
 * @param apiDocUrl   API 文档 URL
 */
public record InlineProviderParams(
        String code,
        String name,
        String description,
        String websiteUrl,
        String apiDocUrl
) {
}
