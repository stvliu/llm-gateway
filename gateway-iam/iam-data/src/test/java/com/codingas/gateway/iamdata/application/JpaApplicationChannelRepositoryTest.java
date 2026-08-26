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
package com.codingas.gateway.iamdata.application;

import com.codingas.gateway.iam.application.ApplicationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaApplicationChannelRepository 单元测试
 *
 * <p>验证应用-渠道授权关联网关行为：渠道 ID 集合查询、关联列表查询、
 * 批量保存与存在性判定。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaApplicationChannelRepository 测试")
class JpaApplicationChannelRepositoryTest {

    @Mock
    private ApplicationChannelJpaRepository repository;

    @InjectMocks
    private JpaApplicationChannelRepository gateway;

    @Nested
    @DisplayName("findChannelIdsByApplicationId 方法测试")
    class FindChannelIdsTests {

        @Test
        @DisplayName("返回去重后的渠道 ID 集合")
        void findChannelIds_returnsDeduplicatedSet() {
            when(repository.findChannelIdsByApplicationId(1L)).thenReturn(List.of(10L, 20L, 10L));

            Set<Long> result = gateway.findChannelIdsByApplicationId(1L);

            assertThat(result).containsExactlyInAnyOrder(10L, 20L);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("无关联时返回空集合")
        void findChannelIds_empty_returnsEmptySet() {
            when(repository.findChannelIdsByApplicationId(2L)).thenReturn(List.of());

            Set<Long> result = gateway.findChannelIdsByApplicationId(2L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByApplicationId 方法测试")
    class FindByApplicationIdTests {

        @Test
        @DisplayName("返回应用下的关联列表（含 priority 映射）")
        void findByApplicationId_returnsList() {
            ApplicationChannelDo d1 = new ApplicationChannelDo();
            d1.setId(1L);
            d1.setApplicationId(1L);
            d1.setChannelId(10L);
            d1.setPriority(1);
            ApplicationChannelDo d2 = new ApplicationChannelDo();
            d2.setId(2L);
            d2.setApplicationId(1L);
            d2.setChannelId(20L);
            d2.setPriority(2);
            when(repository.findByApplicationId(1L)).thenReturn(List.of(d1, d2));

            List<ApplicationChannel> result = gateway.findByApplicationId(1L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(ApplicationChannel::getChannelId)
                    .containsExactly(10L, 20L);
            // priority 应从 DO 映射到实体
            assertThat(result).extracting(ApplicationChannel::getPriority)
                    .containsExactly(1, 2);
        }
    }

    @Nested
    @DisplayName("saveAll 方法测试")
    class SaveAllTests {

        @Test
        @DisplayName("批量保存关联")
        void saveAll_persistsAll() {
            ApplicationChannel rel1 = new ApplicationChannel(1L, 10L);
            rel1.setPriority(1);
            ApplicationChannel rel2 = new ApplicationChannel(1L, 20L);
            rel2.setPriority(2);
            List<ApplicationChannel> rels = List.of(rel1, rel2);
            when(repository.saveAll(anyList())).thenReturn(List.of());

            gateway.saveAll(rels);

            verify(repository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("deleteByApplicationId 方法测试")
    class DeleteByApplicationIdTests {

        @Test
        @DisplayName("按应用 ID 删除全部授权关联，委派至 Repository")
        void deleteByApplicationId_delegatesToRepository() {
            gateway.deleteByApplicationId(1L);

            verify(repository).deleteByApplicationId(1L);
        }
    }
}
