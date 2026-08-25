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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道创建/更新用例入参
 *
 * <p>承载渠道可编辑字段（billingMode 为字符串编码，由核心服务 {@code BillingMode.fromCode} 转换）。
 * 字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.ChannelRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class ChannelCommand {

    /** 所属提供商 ID */
    private final Long providerId;

    /** 渠道名称 */
    private final String name;

    /** 计费模式编码 */
    private final String billingMode;

    /** 配额限制（Token 数） */
    private final Long quotaLimit;

    /** 请求超时秒数 */
    private final Integer timeout;

    /** 最大重试次数 */
    private final Integer maxRetries;
}
