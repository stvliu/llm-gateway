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
package com.codingas.gateway.providerdata.catalog.sync;

import com.codingas.gateway.provider.catalog.sync.CatalogSyncLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaCatalogSyncLogRepository 单元测试：mock JpaRepository 验证委托与
 * CatalogSyncLog ↔ CatalogSyncLogDo 双向转换。
 *
 * <p>覆盖 JpaCatalogSyncLogRepository 全部 public 方法（save/findLatest）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaCatalogSyncLogRepository 单元测试")
class JpaCatalogSyncLogRepositoryTest {

    @Mock
    private CatalogSyncLogJpaRepository jpaRepository;

    @InjectMocks
    private JpaCatalogSyncLogRepository repository;

    private CatalogSyncLog sampleLog(Long id, String message, int added, int updated, int skipped, int failed) {
        CatalogSyncLog log = new CatalogSyncLog();
        log.setId(id);
        log.setTriggeredBy("admin");
        log.setResult("SUCCESS");
        log.setAddedCount(added);
        log.setUpdatedCount(updated);
        log.setSkippedCount(skipped);
        log.setFailedCount(failed);
        log.setMessage(message);
        log.setSyncedAt(Instant.parse("2026-08-28T10:00:00Z"));
        log.setCreatedBy(10L);
        log.setUpdatedBy(20L);
        log.setCreatedAt(Instant.parse("2026-08-28T10:00:00Z"));
        log.setUpdatedAt(Instant.parse("2026-08-28T10:00:00Z"));
        return log;
    }

    private CatalogSyncLogDo sampleDo(Long id, String message, int added, int updated, int skipped, int failed) {
        CatalogSyncLogDo doObj = new CatalogSyncLogDo();
        doObj.setId(id);
        doObj.setTriggeredBy("admin");
        doObj.setResult("SUCCESS");
        doObj.setAddedCount(added);
        doObj.setUpdatedCount(updated);
        doObj.setSkippedCount(skipped);
        doObj.setFailedCount(failed);
        doObj.setMessage(message);
        doObj.setSyncedAt(Instant.parse("2026-08-28T10:00:00Z"));
        doObj.setCreatedBy(10L);
        doObj.setUpdatedBy(20L);
        doObj.setCreatedAt(Instant.parse("2026-08-28T10:00:00Z"));
        doObj.setUpdatedAt(Instant.parse("2026-08-28T10:00:00Z"));
        return doObj;
    }

    @Test
    @DisplayName("save：toDo 写字段 + 委托 save + toEntity 读字段（双向转换）")
    void save_convertsBothWaysAndDelegates() {
        CatalogSyncLog log = sampleLog(1L, "首次全量", 100, 20, 0, 0);
        when(jpaRepository.save(any(CatalogSyncLogDo.class))).thenAnswer(inv -> inv.getArgument(0));

        CatalogSyncLog result = repository.save(log);

        // toEntity 读字段
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTriggeredBy()).isEqualTo("admin");
        assertThat(result.getResult()).isEqualTo("SUCCESS");
        assertThat(result.getAddedCount()).isEqualTo(100);
        assertThat(result.getUpdatedCount()).isEqualTo(20);
        assertThat(result.getSkippedCount()).isZero();
        assertThat(result.getFailedCount()).isZero();
        assertThat(result.getMessage()).isEqualTo("首次全量");
        assertThat(result.getSyncedAt()).isEqualTo(Instant.parse("2026-08-28T10:00:00Z"));

        // toDo 写字段
        ArgumentCaptor<CatalogSyncLogDo> captor = ArgumentCaptor.forClass(CatalogSyncLogDo.class);
        verify(jpaRepository).save(captor.capture());
        CatalogSyncLogDo written = captor.getValue();
        assertThat(written.getTriggeredBy()).isEqualTo("admin");
        assertThat(written.getResult()).isEqualTo("SUCCESS");
        assertThat(written.getAddedCount()).isEqualTo(100);
        assertThat(written.getUpdatedCount()).isEqualTo(20);
        assertThat(written.getSkippedCount()).isZero();
        assertThat(written.getFailedCount()).isZero();
        assertThat(written.getMessage()).isEqualTo("首次全量");
        assertThat(written.getSyncedAt()).isEqualTo(Instant.parse("2026-08-28T10:00:00Z"));
        assertThat(written.getCreatedBy()).isEqualTo(10L);
        assertThat(written.getUpdatedBy()).isEqualTo(20L);
    }

    @Test
    @DisplayName("saveAndFindLatest：连续保存后返回最新一条")
    void saveAndFindLatest_returnsNewest() {
        when(jpaRepository.save(any(CatalogSyncLogDo.class))).thenAnswer(inv -> inv.getArgument(0));
        // 模拟持久化后最新一条为「二次增量」
        when(jpaRepository.findTopByOrderBySyncedAtDesc())
                .thenReturn(Optional.of(sampleDo(2L, "二次增量", 0, 5, 1, 0)));

        repository.save(sampleLog(1L, "首次全量", 100, 20, 0, 0));
        repository.save(sampleLog(2L, "二次增量", 0, 5, 1, 0));

        Optional<CatalogSyncLog> latest = repository.findLatest();

        assertThat(latest).isPresent();
        assertThat(latest.get().getMessage()).isEqualTo("二次增量");
        assertThat(latest.get().getAddedCount()).isEqualTo(0);
        assertThat(latest.get().getUpdatedCount()).isEqualTo(5);
        assertThat(latest.get().getSkippedCount()).isEqualTo(1);
        assertThat(latest.get().getFailedCount()).isZero();
        verify(jpaRepository).findTopByOrderBySyncedAtDesc();
    }

    @Test
    @DisplayName("findLatest：无日志时返回空")
    void findLatest_returnsEmptyWhenNoLogs() {
        when(jpaRepository.findTopByOrderBySyncedAtDesc()).thenReturn(Optional.empty());

        assertThat(repository.findLatest()).isEmpty();
    }
}
