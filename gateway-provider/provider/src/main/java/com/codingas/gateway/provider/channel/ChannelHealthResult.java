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
package com.codingas.gateway.provider.channel;

import com.codingas.gateway.provider.channel.ChannelHealthStatus;

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
