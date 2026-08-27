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
import com.codingas.gateway.iam.user.UserRepository;
import com.codingas.gateway.provider.channel.ChannelRepository;
import com.codingas.gateway.provider.model.ModelRepository;
import com.codingas.gateway.provider.vendor.ProviderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StatsManager 单元测试
 *
 * <p>mock 各域核心 Gateway，断言 getStats 聚合统计与趋势/模型用量统计。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatsManager 测试")
class StatsManagerTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private ModelRepository modelRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CallLogRepository callLogRepository;

    @InjectMocks
    private StatsManager statsManager;

    @Nested
    @DisplayName("getStats 方法测试")
    class GetStatsTests {

        @Test
        @DisplayName("返回四域计数 + 今日调用量与 Token 消耗")
        void getStats_returnsCountsAndTodayUsage() {
            // given
            when(providerRepository.count()).thenReturn(3L);
            when(channelRepository.count()).thenReturn(5L);
            when(modelRepository.count()).thenReturn(10L);
            when(userRepository.count()).thenReturn(42L);
            when(callLogRepository.countSince(any())).thenReturn(128L);
            when(callLogRepository.sumTokensSince(any())).thenReturn(4096L);

            // when
            StatsResult response = statsManager.getStats();

            // then
            assertThat(response.providerCount()).isEqualTo(3L);
            assertThat(response.channelCount()).isEqualTo(5L);
            assertThat(response.modelCount()).isEqualTo(10L);
            assertThat(response.userCount()).isEqualTo(42L);
            assertThat(response.todayRequests()).isEqualTo(128L);
            assertThat(response.tokenUsage()).isEqualTo("4096");
        }

        @Test
        @DisplayName("无数据时所有计数为零")
        void getStats_noData_allZero() {
            // given
            when(providerRepository.count()).thenReturn(0L);
            when(channelRepository.count()).thenReturn(0L);
            when(modelRepository.count()).thenReturn(0L);
            when(userRepository.count()).thenReturn(0L);

            // when
            StatsResult response = statsManager.getStats();

            // then
            assertThat(response.providerCount()).isZero();
            assertThat(response.channelCount()).isZero();
            assertThat(response.modelCount()).isZero();
            assertThat(response.userCount()).isZero();
            assertThat(response.todayRequests()).isZero();
            assertThat(response.tokenUsage()).isEqualTo("0");
        }

        @Test
        @DisplayName("依次调用各 Gateway 的统计方法")
        void getStats_invokesAllGateways() {
            // when
            statsManager.getStats();

            // then
            verify(providerRepository).count();
            verify(channelRepository).count();
            verify(modelRepository).count();
            verify(userRepository).count();
            verify(callLogRepository).countSince(any());
            verify(callLogRepository).sumTokensSince(any());
        }
    }

    @Nested
    @DisplayName("getTrend / getModelUsage 方法测试")
    class TrendAndModelUsageTests {

        @Test
        @DisplayName("getTrend 返回指定天数完整序列，无数据日期补零，最新在末尾")
        void getTrend_fillsMissingDaysWithZero() {
            // given：只有今天有数据
            String today = LocalDate.now().toString();
            when(callLogRepository.findDailyUsage(any(), any()))
                    .thenReturn(List.of(new DailyUsage(today, 5, 100)));

            // when
            List<DailyUsage> trend = statsManager.getTrend(7);

            // then
            assertThat(trend).hasSize(7);
            assertThat(trend.get(6).date()).isEqualTo(today);
            assertThat(trend.get(6).requestCount()).isEqualTo(5);
            assertThat(trend.get(6).tokenCount()).isEqualTo(100);
            // 前 6 天无数据补零
            assertThat(trend.get(0).requestCount()).isZero();
            assertThat(trend.get(0).tokenCount()).isZero();
        }

        @Test
        @DisplayName("getTrend 非正天数回退默认 7 天")
        void getTrend_invalidDays_usesDefault() {
            when(callLogRepository.findDailyUsage(any(), any())).thenReturn(List.of());
            assertThat(statsManager.getTrend(0)).hasSize(7);
        }

        @Test
        @DisplayName("getModelUsage 透传条数上限并返回降序结果")
        void getModelUsage_passesLimitAndReturns() {
            // given
            when(callLogRepository.findModelUsage(anyInt()))
                    .thenReturn(List.of(new ModelUsage("gpt-4o", 10), new ModelUsage("claude-sonnet", 3)));

            // when
            List<ModelUsage> usage = statsManager.getModelUsage(5);

            // then
            assertThat(usage).hasSize(2);
            assertThat(usage.get(0).model()).isEqualTo("gpt-4o");
            assertThat(usage.get(0).requestCount()).isEqualTo(10);
        }
    }
}
