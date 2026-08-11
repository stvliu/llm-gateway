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

import lombok.Data;

import java.time.Instant;

/**
 * 渠道端点响应
 */
@Data
public class ChannelEndpointResponse {

    private Long id;

    private Long channelId;

    private String protocol;

    private String endpointUrl;

    private Instant createdAt;

    private Instant updatedAt;
}
