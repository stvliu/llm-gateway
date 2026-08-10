/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.catalog.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 渠道开通结果
 *
 * <p>封装单个套餐开通（provision）的处理结果。</p>
 */
@Getter
@Builder
public class ProvisionResult {

    /** 套餐编码 */
    private final String planCode;

    /** 开通后的渠道 ID（成功时） */
    private final Long channelId;

    /** 创建的端点数量 */
    private final int endpointCount;

    /** 创建的模型实例数量 */
    private final int instanceCount;

    /** 结果状态：CREATED / SKIPPED / FAILED */
    private final String status;

    /** 错误信息（仅 FAILED 或 SKIPPED 时有值） */
    private final String errorMessage;

    /**
     * 创建成功结果
     */
    public static ProvisionResult created(String planCode, Long channelId, int endpointCount, int instanceCount) {
        return ProvisionResult.builder()
                .planCode(planCode)
                .channelId(channelId)
                .endpointCount(endpointCount)
                .instanceCount(instanceCount)
                .status("CREATED")
                .build();
    }

    /**
     * 创建跳过结果
     */
    public static ProvisionResult skipped(String planCode, String reason) {
        return ProvisionResult.builder()
                .planCode(planCode)
                .status("SKIPPED")
                .errorMessage(reason)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static ProvisionResult failed(String planCode, String error) {
        return ProvisionResult.builder()
                .planCode(planCode)
                .status("FAILED")
                .errorMessage(error)
                .build();
    }
}