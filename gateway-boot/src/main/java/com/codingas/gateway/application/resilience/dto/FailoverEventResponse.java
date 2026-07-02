package com.codingas.gateway.application.resilience.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 转移事件响应 DTO
 *
 * <p>返回转移事件聚合根的完整字段，供容灾总览页轮询渲染转移事件流与耗尽告警。</p>
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

    /** 冗余：失败候选所属故障域 ID（可空） */
    private Long fromClusterId;

    /** 冗余：转移目标所属故障域 ID（可空） */
    private Long toClusterId;

    /** 触发转移的上游错误类型（枚举名） */
    private String errorType;

    /** 转移决策（L1/NONE 枚举名） */
    private String decision;

    /** 是否候选全部耗尽 */
    private boolean exhausted;

    /** 是否共因跳过（true=同域候选被跳过，false=真实失败转移） */
    private boolean commonCauseSkip;

    /** 转移发生时间 */
    private Instant occurredAt;
}
