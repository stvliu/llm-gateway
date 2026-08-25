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
package com.codingas.gateway.providerdata.channel;

import com.codingas.gateway.provider.channel.ChannelOperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaChannelOperationLogRepository 单元测试：mock Repository 验证委托与 domain↔JPA 实体双向转换
 *
 * <p>覆盖 JpaChannelOperationLogRepository 全部 public 方法（save/saveAll/findById/
 * findByChannelId/findByChannelIdAndActions/countByChannelId/findByOperatorId/findByBatchId）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaChannelOperationLogRepository 单元测试")
class ChannelOperationLogGatewayImplTest {

    @Mock
    private ChannelOperationLogJpaRepository repository;

    @InjectMocks
    private JpaChannelOperationLogRepository gateway;

    private ChannelOperationLog sampleLog() {
        ChannelOperationLog log = new ChannelOperationLog();
        log.setChannelId(10L);
        log.setChannelName("主渠道");
        log.setAction("UPDATE");
        log.setActionLabel("更新渠道");
        log.setChangeDetail("{\"name\":\"主渠道\"}");
        log.setOperatorId(100L);
        log.setOperatorName("张三");
        log.setOperatorIp("127.0.0.1");
        log.setTraceId("trace-123");
        log.setOperatedAt(LocalDateTime.parse("2026-08-01T10:00:00"));
        log.setBatchId(500L);
        return log;
    }

    private ChannelOperationLogJpaEntity sampleEntity(Long id) {
        ChannelOperationLogJpaEntity entity = new ChannelOperationLogJpaEntity();
        entity.setId(id);
        entity.setChannelId(10L);
        entity.setChannelName("主渠道");
        entity.setAction("UPDATE");
        entity.setActionLabel("更新渠道");
        entity.setChangeDetail("{\"name\":\"主渠道\"}");
        entity.setOperatorId(100L);
        entity.setOperatorName("张三");
        entity.setOperatorIp("127.0.0.1");
        entity.setTraceId("trace-123");
        entity.setOperatedAt(LocalDateTime.parse("2026-08-01T10:00:00"));
        entity.setBatchId(500L);
        return entity;
    }

    @Test
    @DisplayName("save：fromDomain 写字段 + 委托 save + 回填实体 id")
    void save_delegatesAndBackfillsId() {
        ChannelOperationLog log = sampleLog();
        when(repository.save(any(ChannelOperationLogJpaEntity.class))).thenAnswer(inv -> {
            ChannelOperationLogJpaEntity e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });

        gateway.save(log);

        // 回填 id
        assertThat(log.getId()).isEqualTo(42L);

        // fromDomain 写字段
        ArgumentCaptor<ChannelOperationLogJpaEntity> captor = ArgumentCaptor.forClass(ChannelOperationLogJpaEntity.class);
        verify(repository).save(captor.capture());
        ChannelOperationLogJpaEntity written = captor.getValue();
        assertThat(written.getChannelId()).isEqualTo(10L);
        assertThat(written.getChannelName()).isEqualTo("主渠道");
        assertThat(written.getAction()).isEqualTo("UPDATE");
        assertThat(written.getActionLabel()).isEqualTo("更新渠道");
        assertThat(written.getChangeDetail()).isEqualTo("{\"name\":\"主渠道\"}");
        assertThat(written.getOperatorId()).isEqualTo(100L);
        assertThat(written.getOperatorName()).isEqualTo("张三");
        assertThat(written.getOperatorIp()).isEqualTo("127.0.0.1");
        assertThat(written.getTraceId()).isEqualTo("trace-123");
        assertThat(written.getOperatedAt()).isEqualTo(LocalDateTime.parse("2026-08-01T10:00:00"));
        assertThat(written.getBatchId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("saveAll：批量 fromDomain + 委托 saveAll")
    void saveAll_delegatesAll() {
        List<ChannelOperationLog> logs = List.of(sampleLog(), sampleLog());
        when(repository.saveAll(any())).thenReturn(List.of());

        gateway.saveAll(logs);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChannelOperationLogJpaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).allSatisfy(e -> assertThat(e.getAction()).isEqualTo("UPDATE"));
    }

    @Test
    @DisplayName("findById：存在时 toDomain 读字段，不存在返回空")
    void findById_convertsWhenPresentOrEmpty() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity(1L)));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<ChannelOperationLog> present = gateway.findById(1L);
        assertThat(present).isPresent();
        assertThat(present.get().getId()).isEqualTo(1L);
        assertThat(present.get().getChannelId()).isEqualTo(10L);
        assertThat(present.get().getChannelName()).isEqualTo("主渠道");
        assertThat(present.get().getAction()).isEqualTo("UPDATE");
        assertThat(present.get().getActionLabel()).isEqualTo("更新渠道");
        assertThat(present.get().getChangeDetail()).isEqualTo("{\"name\":\"主渠道\"}");
        assertThat(present.get().getOperatorId()).isEqualTo(100L);
        assertThat(present.get().getOperatorName()).isEqualTo("张三");
        assertThat(present.get().getOperatorIp()).isEqualTo("127.0.0.1");
        assertThat(present.get().getTraceId()).isEqualTo("trace-123");
        assertThat(present.get().getOperatedAt()).isEqualTo(LocalDateTime.parse("2026-08-01T10:00:00"));
        assertThat(present.get().getBatchId()).isEqualTo(500L);

        assertThat(gateway.findById(99L)).isEmpty();
    }

    @Test
    @DisplayName("findByChannelId：分页查询并 toDomain 转换")
    void findByChannelId_pagedAndConverts() {
        when(repository.findByChannelIdOrderByOperatedAtDesc(10L, PageRequest.of(0, 20)))
                .thenReturn(List.of(sampleEntity(1L), sampleEntity(2L)));

        List<ChannelOperationLog> result = gateway.findByChannelId(10L, 0, 20);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ChannelOperationLog::getId).containsExactly(1L, 2L);
        verify(repository).findByChannelIdOrderByOperatedAtDesc(eq(10L), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("findByChannelIdAndActions：按操作集合分页查询并转换")
    void findByChannelIdAndActions_pagedAndConverts() {
        List<String> actions = List.of("UPDATE", "DELETE");
        when(repository.findByChannelIdAndActionInOrderByOperatedAtDesc(10L, actions, PageRequest.of(1, 10)))
                .thenReturn(List.of(sampleEntity(3L)));

        List<ChannelOperationLog> result = gateway.findByChannelIdAndActions(10L, actions, 1, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAction()).isEqualTo("UPDATE");
        verify(repository).findByChannelIdAndActionInOrderByOperatedAtDesc(
                eq(10L), eq(actions), eq(PageRequest.of(1, 10)));
    }

    @Test
    @DisplayName("countByChannelId：委托 Repository 统计")
    void countByChannelId_returnsRepositoryCount() {
        when(repository.countByChannelId(10L)).thenReturn(5L);
        assertThat(gateway.countByChannelId(10L)).isEqualTo(5L);
    }

    @Test
    @DisplayName("findByOperatorId：按操作人分页查询并转换")
    void findByOperatorId_pagedAndConverts() {
        when(repository.findByOperatorIdOrderByOperatedAtDesc(100L, PageRequest.of(0, 20)))
                .thenReturn(List.of(sampleEntity(1L)));

        List<ChannelOperationLog> result = gateway.findByOperatorId(100L, 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOperatorId()).isEqualTo(100L);
        verify(repository).findByOperatorIdOrderByOperatedAtDesc(eq(100L), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("findByBatchId：按批次查询并转换")
    void findByBatchId_convertsMatches() {
        when(repository.findByBatchIdOrderByOperatedAtDesc(500L)).thenReturn(List.of(sampleEntity(1L)));

        List<ChannelOperationLog> result = gateway.findByBatchId(500L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBatchId()).isEqualTo(500L);
        verify(repository).findByBatchIdOrderByOperatedAtDesc(500L);
    }
}
