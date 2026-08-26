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

import com.codingas.gateway.common.entity.BaseEntity;
import com.codingas.gateway.provider.upstream.Protocol;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 渠道端点实体
 *
 * <p>一个 ChannelEndpoint 声明一个协议端点——只回答"用什么协议、调哪个 URL"。</p>
 * <p>一个 Channel 可拥有多个 ChannelEndpoint（如火山引擎 Coding Plan 同时提供 OpenAI 和 Anthropic 两个端点）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ChannelEndpoint extends BaseEntity {

    /** 所属渠道 ID */
    private Long channelId;

    /** 协议类型 */
    private Protocol protocol;

    /** 端点 URL */
    private String endpointUrl;
}
