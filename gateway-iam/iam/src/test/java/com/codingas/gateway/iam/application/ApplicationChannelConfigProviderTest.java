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
package com.codingas.gateway.iam.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApplicationChannelConfigProvider（应用渠道配置缓存）测试")
class ApplicationChannelConfigProviderTest {

    @Mock
    private ApplicationChannelRepository repository;

    private ApplicationChannelConfigProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ApplicationChannelConfigProvider(repository);
    }

    private ApplicationChannel channel(long id, Integer priority) {
        ApplicationChannel ch = new ApplicationChannel();
        ch.setChannelId(id);
        ch.setPriority(priority);
        return ch;
    }

    @Test
    @DisplayName("findChannelIdsByApplicationId：首次查仓储，命中后不再查询")
    void channelIds_firstLoad_thenCached() {
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L, 2L));

        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        verify(repository, times(1)).findChannelIdsByApplicationId(100L);
    }

    @Test
    @DisplayName("findChannelIdsByApplicationId：applicationId 为 null 返回空集，不查仓储不缓存")
    void channelIds_nullApplicationId_returnsEmpty() {
        assertThat(provider.findChannelIdsByApplicationId(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("findPriorityMapByApplicationId：按优先级构建映射并缓存（null 优先级剔除）")
    void priorityMap_buildsAndCaches() {
        when(repository.findByApplicationId(100L)).thenReturn(List.of(channel(1L, 10), channel(2L, null)));

        assertThat(provider.findPriorityMapByApplicationId(100L))
                .containsExactlyEntriesOf(Map.of(1L, 10));
        assertThat(provider.findPriorityMapByApplicationId(100L))
                .containsExactlyEntriesOf(Map.of(1L, 10));
        verify(repository, times(1)).findByApplicationId(100L);
    }

    @Test
    @DisplayName("findPriorityMapByApplicationId：applicationId 为 null 返回空映射")
    void priorityMap_nullApplicationId_returnsEmpty() {
        assertThat(provider.findPriorityMapByApplicationId(null)).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("evict：失效后重新查询仓储（配置变更即时生效）")
    void evict_reloadsFromRepository() {
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L));
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactly(1L);

        provider.evict(100L);
        when(repository.findChannelIdsByApplicationId(100L)).thenReturn(Set.of(1L, 2L));
        assertThat(provider.findChannelIdsByApplicationId(100L)).containsExactlyInAnyOrder(1L, 2L);
        verify(repository, times(2)).findChannelIdsByApplicationId(100L);
    }
}
