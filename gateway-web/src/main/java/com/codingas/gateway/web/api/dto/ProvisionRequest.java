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
package com.codingas.gateway.web.api.dto;

import com.codingas.gateway.provider.channel.InlineProviderParams;
import lombok.Data;

import java.util.List;

/**
 * 渠道开通请求 DTO（HTTP 契约）
 *
 * <p>支持批量创建 API Key 凭证，以及在 provider 不存在时通过 inlineProvider 内联创建供应商。</p>
 */
@Data
public class ProvisionRequest {

    /** API Key 列表（批量创建凭证） */
    private List<String> apiKeys;

    /**
     * 内联供应商信息（可选）
     *
     * <p>仅在目标 providerCode 尚未持久化时生效；若 providerCode 已存在，则该字段被忽略。</p>
     */
    private InlineProvider inlineProvider;

    /**
     * 内联供应商参数
     *
     * @param code        供应商程序标识，必须与 planCode 解析出的 providerCode 一致
     * @param name        显示名（缺省时回退为 code）
     * @param description 描述
     * @param websiteUrl  官网 URL
     * @param apiDocUrl   API 文档 URL
     */
    public record InlineProvider(
            String code,
            String name,
            String description,
            String websiteUrl,
            String apiDocUrl
    ) {
    }

    /**
     * 转换为内联供应商参数
     *
     * @return 内联供应商参数（无内联时为 null）
     */
    public InlineProviderParams toInlineParams() {
        if (inlineProvider == null) {
            return null;
        }
        return new InlineProviderParams(
                inlineProvider.code(), inlineProvider.name(), inlineProvider.description(),
                inlineProvider.websiteUrl(), inlineProvider.apiDocUrl());
    }
}
