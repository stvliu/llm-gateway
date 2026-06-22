package com.codingas.gateway.application.application.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 应用响应 DTO
 *
 * <p>返回应用聚合根的完整字段，含预留的容灾画像/配额预算/看板 ID。</p>
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

    /** 容灾画像 ID（预留） */
    private Long resilienceProfileId;

    /** 配额预算 ID（预留） */
    private Long quotaBudgetId;

    /** 看板 ID（预留） */
    private Long dashboardId;

    /** 创建时间 */
    private Instant createdAt;

    /** 更新时间 */
    private Instant updatedAt;
}
