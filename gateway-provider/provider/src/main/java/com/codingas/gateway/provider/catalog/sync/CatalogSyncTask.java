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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 模型目录自动同步任务
 *
 * <p>每小时检查一次是否需要执行 models.dev 目录同步：先读 {@code catalog.sync.enabled}
 * 开关（默认开启，关闭则跳过），再读 {@code catalog.sync.interval} 周期（DAILY=24h /
 * WEEKLY=7d / MONTHLY=30d），最后按最近一次同步时间（{@link CatalogSyncLogRepository#findLatest}，
 * 无记录视为需同步）判断是否达到间隔。同步失败仅记录 error 日志，不向调度器抛出异常。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogSyncTask {

    /** 目录同步编排服务 */
    private final ModelCatalogSyncService syncService;

    /** 同步日志 Repository（查询最近同步时间） */
    private final CatalogSyncLogRepository logRepository;

    /** 系统设置服务（读取同步开关与周期） */
    private final SystemSettingService settingService;

    /**
     * 定时检查是否需要执行目录同步
     *
     * <p>开关关闭或未达同步间隔时跳过并输出 debug 日志；达到间隔时调用
     * {@link ModelCatalogSyncService#sync()}，同步失败捕获 {@link RuntimeException}
     * 记录 error 日志，避免中断调度线程。</p>
     */
    @Scheduled(fixedRate = 3600_000) // 每小时检查一次
    public void check() {
        if (!settingService.getBoolean("catalog.sync.enabled", true)) {
            log.debug("模型目录自动同步已关闭，跳过");
            return;
        }
        SyncInterval interval = settingService.getEnum(
                "catalog.sync.interval", SyncInterval.class, SyncInterval.DAILY);
        long thresholdHours = switch (interval) {
            case DAILY -> 24;
            case WEEKLY -> 24 * 7;
            case MONTHLY -> 24 * 30;
        };
        Optional<CatalogSyncLog> latest = logRepository.findLatest();
        boolean shouldSync = latest.isEmpty()
                || latest.get().getSyncedAt() == null
                || latest.get().getSyncedAt().isBefore(Instant.now().minus(thresholdHours, ChronoUnit.HOURS));
        if (!shouldSync) {
            log.debug("距上次同步未达间隔({}), 跳过", interval);
            return;
        }
        try {
            CatalogSyncReport report = syncService.sync();
            log.info("自动同步完成: added={}, updated={}", report.getAddedCount(), report.getUpdatedCount());
        } catch (RuntimeException e) {
            log.error("自动同步失败: {}", e.getMessage(), e);
        }
    }
}
