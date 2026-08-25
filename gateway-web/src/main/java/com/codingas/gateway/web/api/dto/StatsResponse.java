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

import com.codingas.gateway.stats.StatsResult;

/**
 * 系统统计数据响应 DTO（HTTP 契约）
 *
 * @param providerCount 提供商数量
 * @param channelCount  渠道数量
 * @param modelCount    模型数量
 * @param userCount     用户数量
 * @param todayRequests 今日请求数
 * @param tokenUsage    Token 用量
 */
public record StatsResponse(
    long providerCount,
    long channelCount,
    long modelCount,
    long userCount,
    long todayRequests,
    String tokenUsage
) {
    /**
     * 从统计用例结果转换
     *
     * @param result 统计用例结果
     * @return 统计响应 DTO
     */
    public static StatsResponse from(StatsResult result) {
        return new StatsResponse(
                result.providerCount(),
                result.channelCount(),
                result.modelCount(),
                result.userCount(),
                result.todayRequests(),
                result.tokenUsage());
    }
}
