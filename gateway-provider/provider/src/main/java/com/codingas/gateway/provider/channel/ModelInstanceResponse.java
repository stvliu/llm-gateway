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
 * 模型实例响应
 */
@Data
public class ModelInstanceResponse {

    private Long id;

    private Long channelId;

    private Long modelId;

    /** 供应商侧模型名称 */
    private String modelName;

    /** 模型展示名称 */
    private String displayName;

    /** 模型系列 */
    private String modelFamily;

    /** 上游模型名（为 null 表示与 modelName 相同） */
    private String upstreamModelName;

    /** 优先级 */
    private Integer priority;

    /** 权重 */
    private Integer weight;

    /** 关联状态 */
    private String state;
}