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

import java.util.List;

/**
 * 渠道领域视图对象（读模型）
 *
 * <p>由核心应用服务组装：携带 {@link Channel} 实体及其展示所需的关联数据
 * （提供商名称、端点列表），供 web 层 DTO 纯映射，避免 DTO 转换层依赖持久化仓储。</p>
 */
@Getter
@AllArgsConstructor
public class ChannelView {

    /** 渠道实体 */
    private final Channel channel;

    /** 提供商名称（仅展示用，来自 Provider 关联） */
    private final String providerName;

    /** 渠道端点列表 */
    private final List<ChannelEndpoint> endpoints;
}
