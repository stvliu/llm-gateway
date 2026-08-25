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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 应用渠道授权更新请求 DTO
 *
 * <p>承载应用授权的渠道及其应用级转移优先级列表，用于 PUT /api/v1/applications/{id}/channels。</p>
 *
 * <p>Task gap2：转移顺序改为应用级 priority，本请求由纯 channelIds 升级为
 * 含 priority 的列表。空列表表示清空全部授权；每个元素的 channelId 必须非 null 且为正数。</p>
 *
 * @param channels 渠道授权项列表（channelId + priority）
 */
public record ApplicationChannelRequest(
        @NotNull(message = "channels 不能为 null，空列表请传 []")
        List<@Valid ApplicationChannelItem> channels
) {
}
