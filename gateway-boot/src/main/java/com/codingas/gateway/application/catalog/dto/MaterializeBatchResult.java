package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 批量物化结果
 *
 * <p>封装 Provider 级联物化时多条 Plan 的物化结果统计。</p>
 */
@Getter
@Builder
public class MaterializeBatchResult {

    /** 供应商编码 */
    private final String providerCode;

    /** 本次处理总条目 */
    private final int totalCount;

    /** 成功数 */
    private final int successCount;

    /** 跳过数（已物化） */
    private final int skippedCount;

    /** 失败数 */
    private final int failedCount;

    /** 每条 Plan 的独立物化结果 */
    private final List<PlanResult> results;
}