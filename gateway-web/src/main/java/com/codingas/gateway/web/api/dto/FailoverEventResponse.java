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

import com.codingas.gateway.resilience.failover.FailoverEvent;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 转移事件响应 DTO（HTTP 契约）
 *
 * <p>返回转移事件根实体的完整字段，供容灾总览页轮询渲染转移事件流与耗尽告警。</p>
 *
 * <p>errorType / decision 以字符串返回（枚举名），前端按字符串展示，避免耦合枚举类型。</p>
 */
@Data
public class FailoverEventResponse {

    /** 事件 ID */
    private Long id;

    /** OpenTelemetry Trace ID */
    private String traceId;

    /** 应用 ID */
    private Long applicationId;

    /** 失败候选的渠道 ID */
    private Long fromChannelId;

    /** 失败候选的端点 ID */
    private Long fromEndpointId;

    /** 转移目标候选的渠道 ID（exhausted 时为 null） */
    private Long toChannelId;

    /** 转移目标候选的端点 ID（exhausted 时为 null） */
    private Long toEndpointId;

    /** 触发转移的上游错误类型（枚举名） */
    private String errorType;

    /** 转移决策（L1/NONE 枚举名） */
    private String decision;

    /** 是否候选全部耗尽 */
    private boolean exhausted;

    /** 转移发生时间 */
    private Instant occurredAt;

    /**
     * 从转移事件实体转换（枚举字段转字符串展示）
     *
     * @param event 转移事件实体
     * @return 转移事件响应 DTO
     */
    public static FailoverEventResponse from(FailoverEvent event) {
        FailoverEventResponse response = new FailoverEventResponse();
        response.setId(event.getId());
        response.setTraceId(event.getTraceId());
        response.setApplicationId(event.getApplicationId());
        response.setFromChannelId(event.getFromChannelId());
        response.setFromEndpointId(event.getFromEndpointId());
        response.setToChannelId(event.getToChannelId());
        response.setToEndpointId(event.getToEndpointId());
        response.setErrorType(event.getErrorType() != null ? event.getErrorType().name() : null);
        response.setDecision(event.getDecision() != null ? event.getDecision().name() : null);
        response.setExhausted(event.isExhausted());
        response.setOccurredAt(event.getOccurredAt());
        return response;
    }

    /**
     * 从转移事件实体列表转换
     *
     * @param events 转移事件实体列表
     * @return 转移事件响应 DTO 列表
     */
    public static List<FailoverEventResponse> from(List<FailoverEvent> events) {
        return events.stream().map(FailoverEventResponse::from).toList();
    }
}
