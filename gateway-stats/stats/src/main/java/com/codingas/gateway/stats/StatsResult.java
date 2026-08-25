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
package com.codingas.gateway.stats;

/**
 * 系统统计数据用例结果
 *
 * @param providerCount 提供商数量
 * @param channelCount  渠道数量
 * @param modelCount    模型数量
 * @param userCount     用户数量
 * @param todayRequests 今日请求数
 * @param tokenUsage    Token 用量
 */
public record StatsResult(
        long providerCount,
        long channelCount,
        long modelCount,
        long userCount,
        long todayRequests,
        String tokenUsage
) {
}
