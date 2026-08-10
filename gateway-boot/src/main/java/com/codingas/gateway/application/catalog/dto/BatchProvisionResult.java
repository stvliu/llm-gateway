/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 批量开通结果
 *
 * <p>封装供应商级联开通时多条套餐的处理结果统计。</p>
 */
@Getter
@Builder
public class BatchProvisionResult {

    /** 供应商编码 */
    private final String providerCode;

    /** 本次处理总条目 */
    private final int totalCount;

    /** 成功数 */
    private final int successCount;

    /** 跳过数（已开通） */
    private final int skippedCount;

    /** 失败数 */
    private final int failedCount;

    /** 每条套餐的独立开通结果 */
    private final List<ProvisionResult> results;
}