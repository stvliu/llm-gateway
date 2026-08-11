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
package com.codingas.gateway.application.provider.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 提供商响应
 *
 * <p>注意：Key 统计信息已移除，API Key 管理迁移到 ProductApiKey。</p>
 */
@Data
public class ProviderResponse {

    private Long id;
    /** 品牌标识（如 openai、anthropic），用于前端图标渲染 */
    private String providerId;
    private String providerName;
    private String description;
    private String websiteUrl;
    private String apiDocUrl;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;
}