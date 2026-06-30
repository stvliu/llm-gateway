package com.codingas.gateway.application.application.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 应用响应 DTO
 *
 * <p>返回应用聚合根的完整字段，含应用级 timeout 与预留的配额预算/看板 ID。</p>
 *
 * <p>Task 8：{@code resilienceProfileId} 退场，改为 {@code timeout}（承接原 ResilienceProfile.timeout）。</p>
 */
@Data
public class ApplicationResponse {

    /** 应用 ID */
    private Long id;

    /** 应用编码，全局唯一 */
    private String code;

    /** 应用名称 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 应用生命周期状态（ACTIVE/INACTIVE） */
    private String state;

    /** 请求超时秒数（0 表示用渠道默认；承接原 ResilienceProfile.timeout） */
    private int timeout;

    /** 配额预算 ID（预留） */
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    private Long dashboardId;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
