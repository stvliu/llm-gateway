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
 * CatalogProbeTask 单元测试
 *
 * <p>验证上游列表探测的调度判定：探测开关关闭跳过、未达周期跳过、
 * 无历史记录（达周期）时执行探测；周期仅按 PROBE 来源日志判断，
 * 目录同步日志不抑制探测。</p>
 */
@ExtendWith(MockitoExtension.class)
class CatalogProbeTaskTest {

    @Mock private CatalogProbeService probeService;
    @Mock private CatalogSyncLogRepository logRepository;
    @Mock private SystemSettingService settingService;

    private CatalogProbeTask task;

    @BeforeEach
    void setUp() {
        task = new CatalogProbeTask(probeService, logRepository, settingService);
    }

    @Test
    @DisplayName("探测开关关闭时跳过")
    void probeDisabled_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(false);

        task.check();

        verify(probeService, never()).probe();
    }

    @Test
    @DisplayName("未达探测周期时跳过")
    void notDue_skips() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.deprecation.probe.interval",
                SyncInterval.class, SyncInterval.WEEKLY)).thenReturn(SyncInterval.WEEKLY);
        CatalogSyncLog log = new CatalogSyncLog();
        log.setSyncedAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(logRepository.findLatestByTriggeredBy("PROBE")).thenReturn(Optional.of(log));

        task.check();

        verify(probeService, never()).probe();
    }

    @Test
    @DisplayName("达周期时执行探测")
    void due_executesProbe() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.deprecation.probe.interval",
                SyncInterval.class, SyncInterval.WEEKLY)).thenReturn(SyncInterval.WEEKLY);
        when(logRepository.findLatestByTriggeredBy("PROBE")).thenReturn(Optional.empty());
        when(probeService.probe()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(probeService).probe();
    }

    @Test
    @DisplayName("最近同步日志较新但探测日志已过期时仍执行探测")
    void syncLogFreshButProbeDue_executesProbe() {
        when(settingService.getBoolean("catalog.deprecation.enabled", true)).thenReturn(true);
        when(settingService.getBoolean("catalog.deprecation.probe.enabled", true)).thenReturn(true);
        when(settingService.getEnum("catalog.deprecation.probe.interval",
                SyncInterval.class, SyncInterval.WEEKLY)).thenReturn(SyncInterval.WEEKLY);
        // 最近探测日志已超过 7 天（过期），即使存在较新的同步日志也应执行探测
        CatalogSyncLog probeLog = new CatalogSyncLog();
        probeLog.setSyncedAt(Instant.now().minus(8, ChronoUnit.DAYS));
        when(logRepository.findLatestByTriggeredBy("PROBE")).thenReturn(Optional.of(probeLog));
        when(probeService.probe()).thenReturn(CatalogSyncReport.builder().success(true).build());

        task.check();

        verify(probeService).probe();
    }
}
