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
package com.codingas.gateway.provider.catalog.sync;

import com.codingas.gateway.settings.SyncInterval;
import com.codingas.gateway.settings.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CatalogSyncTask 单元测试
 *
 * <p>验证模型目录自动同步的调度判定：开关关闭跳过、达到间隔触发同步、
 * 未达间隔跳过、无历史记录触发首次同步。</p>
 */
@ExtendWith(MockitoExtension.class)
class CatalogSyncTaskTest {

    @Mock private ModelCatalogSyncService syncService;
    @Mock private CatalogSyncLogRepository logRepository;
    @Mock private SystemSettingService settingService;

    private CatalogSyncTask task;

    @BeforeEach
    void setUp() {
        task = new CatalogSyncTask(syncService, logRepository, settingService);
    }

    @Test
    @DisplayName("开关关闭时跳过同步")
    void check_syncDisabled_skips() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(false);

        task.check();

        verify(syncService, never()).sync();
    }

    @Test
    @DisplayName("达到间隔时触发同步")
    void check_intervalElapsed_triggersSync() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        CatalogSyncLog log = new CatalogSyncLog();
        log.setSyncedAt(Instant.now().minus(25, ChronoUnit.HOURS));
        when(logRepository.findLatest()).thenReturn(Optional.of(log));
        when(syncService.sync()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(syncService).sync();
    }

    @Test
    @DisplayName("未达到间隔时跳过同步")
    void check_intervalNotElapsed_skips() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        CatalogSyncLog log = new CatalogSyncLog();
        log.setSyncedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(logRepository.findLatest()).thenReturn(Optional.of(log));

        task.check();

        verify(syncService, never()).sync();
    }

    @Test
    @DisplayName("无历史同步记录时触发首次同步")
    void check_noHistory_triggers() {
        when(settingService.getBoolean("catalog.sync.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY))
                .thenReturn(SyncInterval.DAILY);
        when(logRepository.findLatest()).thenReturn(Optional.empty());
        when(syncService.sync()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(syncService).sync();
    }
}
