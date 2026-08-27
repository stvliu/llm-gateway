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

import com.codingas.gateway.audit.CallLogRepository;
import com.codingas.gateway.audit.DailyUsage;
import com.codingas.gateway.audit.ModelUsage;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import com.codingas.gateway.iam.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表服务
 *
 * <p>通过各域核心 Gateway 端口获取统计计数（端口调用，不依赖绑定模块 Repository）。
 * 调用量/Token 统计数据源为 audit 域 CallLog（call_logs 表）。</p>
 */
@Service
@RequiredArgsConstructor
public class StatsManager {

    private final ProviderRepository providerRepository;
    private final ChannelRepository channelRepository;
    private final ModelRepository modelRepository;
    private final UserRepository userRepository;
    private final CallLogRepository callLogRepository;

    /**
     * 获取系统统计数据
     */
    @Transactional(readOnly = true)
    public StatsResult getStats() {
        long providerCount = providerRepository.count();
        long channelCount = channelRepository.count();
        long modelCount = modelRepository.count();
        long userCount = userRepository.count();
        // 今日调用量与 Token 消耗（基于 call_logs）
        Instant todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        long todayRequests = callLogRepository.countSince(todayStart);
        long todayTokens = callLogRepository.sumTokensSince(todayStart);
        return new StatsResult(
                providerCount,
                channelCount,
                modelCount,
                userCount,
                todayRequests,
                String.valueOf(todayTokens)
        );
    }

    /**
     * 获取最近 N 天调用趋势（按天，含无数据日期补零，从旧到新）
     *
     * @param days 天数
     * @return 按天用量列表
     */
    @Transactional(readOnly = true)
    public List<DailyUsage> getTrend(int days) {
        if (days <= 0) {
            days = 7;
        }
        LocalDate today = LocalDate.now();
        Instant start = today.minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant end = today.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

        Map<String, DailyUsage> byDate = callLogRepository.findDailyUsage(start, end).stream()
                .collect(Collectors.toMap(DailyUsage::date, d -> d));

        List<DailyUsage> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            DailyUsage usage = byDate.get(date);
            result.add(usage != null ? usage : new DailyUsage(date, 0, 0));
        }
        return result;
    }

    /**
     * 获取模型调用量分布（Top N）
     *
     * @param limit 条数上限
     * @return 按请求数降序的模型用量
     */
    @Transactional(readOnly = true)
    public List<ModelUsage> getModelUsage(int limit) {
        return callLogRepository.findModelUsage(limit > 0 ? limit : 5);
    }
}
