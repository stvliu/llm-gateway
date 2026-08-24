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
package com.codingas.gateway.providerdata.gateway;

import com.codingas.gateway.providerdata.repository.ChannelRepository;
import com.codingas.gateway.providerdata.repository.ModelRepository;
import com.codingas.gateway.providerdata.repository.ProviderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 各域 Gateway count 统计端口测试
 *
 * <p>验证 Provider/Model/Channel GatewayImpl 的 count() 均委托对应 Repository 统计并原样返回。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("各域 Gateway count 统计端口测试")
class CountPortTest {

    @Nested
    @DisplayName("ProviderGateway count 测试")
    class ProviderCountTests {

        @Mock
        private ProviderRepository providerRepository;

        @InjectMocks
        private ProviderGatewayImpl gateway;

        @Test
        @DisplayName("count 返回供应商总数")
        void count_returnsRepositoryCount() {
            // given
            when(providerRepository.count()).thenReturn(42L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("ModelGateway count 测试")
    class ModelCountTests {

        @Mock
        private ModelRepository modelRepository;

        @InjectMocks
        private ModelGatewayImpl gateway;

        @Test
        @DisplayName("count 返回模型总数")
        void count_returnsRepositoryCount() {
            // given
            when(modelRepository.count()).thenReturn(7L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("ChannelGateway count 测试")
    class ChannelCountTests {

        @Mock
        private ChannelRepository channelRepository;

        @InjectMocks
        private ChannelGatewayImpl gateway;

        @Test
        @DisplayName("count 返回渠道总数")
        void count_returnsRepositoryCount() {
            // given
            when(channelRepository.count()).thenReturn(15L);

            // when
            long result = gateway.count();

            // then
            assertThat(result).isEqualTo(15L);
        }
    }
}
