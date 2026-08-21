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
package com.codingas.gateway.domain.audit.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 调用日志实体
 *
 * <p>记录每次模型调用的全链路信息。</p>
 */
@Getter
@Setter
public class CallLog {

    private Long id;
    private String traceId;
    private Long userId;
    private String model;
    private Long channelId;
    private Long channelEndpointId;
    private String inboundProtocol;
    private String upstreamProtocol;
    private Long durationMs;
    private Boolean success;
    private Integer inputTokens;
    private Integer outputTokens;
    private String errorMessage;
    private Instant calledAt;
}