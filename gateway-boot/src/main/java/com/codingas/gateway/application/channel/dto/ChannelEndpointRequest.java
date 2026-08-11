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
package com.codingas.gateway.application.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 渠道端点创建/更新请求
 *
 * <p>注意：channelId 由适配层（Controller/gRPC stub）从协议上下文中提取并填充，
 * 请求体本身不包含此字段。</p>
 */
@Data
public class ChannelEndpointRequest {

    /** 渠道 ID（适配层填充） */
    private Long channelId;

    @NotNull(message = "协议类型不能为空")
    private String protocol;

    @NotBlank(message = "端点 URL 不能为空")
    private String endpointUrl;
}
