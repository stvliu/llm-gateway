/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.supply.dto;

import com.codingas.gateway.domain.supply.enums.ChannelHealthStatus;

import java.time.Instant;
import java.util.List;

/**
 * 渠道健康测试结果
 *
 * @param channelId       渠道 ID
 * @param aggregateStatus 聚合后的健康状态
 * @param startedAt       测试开始时间
 * @param finishedAt      测试结束时间
 * @param matrix          单 Key 测试矩阵
 */
public record ChannelHealthResult(
        Long channelId,
        ChannelHealthStatus aggregateStatus,
        Instant startedAt,
        Instant finishedAt,
        List<KeyMatrixRow> matrix
) {
}
