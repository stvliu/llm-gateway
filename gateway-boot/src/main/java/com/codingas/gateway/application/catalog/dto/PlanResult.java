package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 单条 Plan 物化结果
 *
 * <p>级联物化中每条 Plan 的独立处理结果。</p>
 */
@Getter
@Builder
public class PlanResult {

    /** 物化类型：PLAN */
    private final String type;

    /** Plan 编码 */
    private final String planCode;

    /** 物化后 Channel 实体 ID（成功时） */
    private final Long entityId;

    /** 结果状态：CREATED / SKIPPED / FAILED */
    private final String status;

    /** 失败原因（仅 FAILED 时有值） */
    private final String errorMessage;
}