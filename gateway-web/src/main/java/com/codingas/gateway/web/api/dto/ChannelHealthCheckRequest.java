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

import com.codingas.gateway.provider.channel.ChannelHealthSource;
import jakarta.validation.constraints.NotNull;

/**
 * 渠道健康检查请求体
 *
 * @param source 触发来源（CARD / DRAWER / PRECHECK），必填
 */
public record ChannelHealthCheckRequest(
        @NotNull(message = "source 字段必填") ChannelHealthSource source
) {
}
