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

import lombok.Data;

/**
 * 模型实例更新请求
 *
 * <p>支持更新模型 ID 和上游模型名。channelId 由适配层（Controller）从协议上下文中提取并填充。
 * 字段为 null 表示不更新该字段。</p>
 */
@Data
public class ModelInstanceUpdateRequest {

    /** 渠道 ID（适配层填充） */
    private Long channelId;

    /** 新模型 ID，为 null 表示不更新 */
    private Long modelId;

    /** 上游模型名，为 null 表示不更新 */
    private String upstreamModelName;
}
