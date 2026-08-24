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

import com.codingas.gateway.iam.user.UserGateway;
import com.codingas.gateway.provider.channel.ChannelGateway;
import com.codingas.gateway.provider.model.ModelGateway;
import com.codingas.gateway.provider.vendor.ProviderGateway;
import com.codingas.gateway.stats.dto.StatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * StatsService 单元测试
 *
 * <p>mock 4 个域核心 Gateway，断言 getStats 聚合统计结果。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatsService 测试")
class StatsServiceTest {

    @Mock
    private ProviderGateway providerGateway;

    @Mock
    private ChannelGateway channelGateway;

    @Mock
    private ModelGateway modelGateway;

    @Mock
    private UserGateway userGateway;

    @InjectMocks
    private StatsService statsService;

    @Nested
    @DisplayName("getStats 方法测试")
    class GetStatsTests {

        @Test
        @DisplayName("返回四个域的计数统计")
        void getStats_returnsCounts() {
            // given
            when(providerGateway.count()).thenReturn(3L);
            when(channelGateway.count()).thenReturn(5L);
            when(modelGateway.count()).thenReturn(10L);
            when(userGateway.count()).thenReturn(42L);

            // when
            StatsResponse response = statsService.getStats();

            // then
            assertThat(response.providerCount()).isEqualTo(3L);
            assertThat(response.channelCount()).isEqualTo(5L);
            assertThat(response.modelCount()).isEqualTo(10L);
            assertThat(response.userCount()).isEqualTo(42L);
            // 请求统计与 Token 用量尚未接入真实数据
            assertThat(response.todayRequests()).isZero();
            assertThat(response.tokenUsage()).isEqualTo("0");
        }

        @Test
        @DisplayName("无数据时所有计数为零")
        void getStats_noData_allZero() {
            // given
            when(providerGateway.count()).thenReturn(0L);
            when(channelGateway.count()).thenReturn(0L);
            when(modelGateway.count()).thenReturn(0L);
            when(userGateway.count()).thenReturn(0L);

            // when
            StatsResponse response = statsService.getStats();

            // then
            assertThat(response.providerCount()).isZero();
            assertThat(response.channelCount()).isZero();
            assertThat(response.modelCount()).isZero();
            assertThat(response.userCount()).isZero();
        }

        @Test
        @DisplayName("依次调用四个 Gateway 的 count 方法")
        void getStats_invokesAllGateways() {
            // when
            statsService.getStats();

            // then
            verify(providerGateway).count();
            verify(channelGateway).count();
            verify(modelGateway).count();
            verify(userGateway).count();
        }
    }
}
