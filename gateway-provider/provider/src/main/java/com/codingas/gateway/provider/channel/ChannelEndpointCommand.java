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
 * 渠道端点创建/更新用例入参
 *
 * <p>protocol 为协议编码字符串，由核心服务 {@code Protocol.fromCode} 转换。
 * 字段合法性校验由 HTTP 层 DTO 承担（{@code web.api.dto.ChannelEndpointRequest}）。</p>
 */
@Getter
@AllArgsConstructor
public class ChannelEndpointCommand {

    /** 所属渠道 ID */
    private final Long channelId;

    /** 协议编码（如 openai/anthropic） */
    private final String protocol;

    /** 端点 URL */
    private final String endpointUrl;
}
