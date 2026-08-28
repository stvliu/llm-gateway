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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * 上游列表探测定时任务
 *
 * <p>每小时检查一次是否需要执行探测：先读 {@code catalog.deprecation.enabled} 与
 * {@code catalog.deprecation.probe.enabled}（关闭则跳过），再读
 * {@code catalog.deprecation.probe.interval} 周期（DAILY=24h / WEEKLY=7d / MONTHLY=30d），
 * 按最近一次探测时间（{@link CatalogSyncLogRepository#findLatestByTriggeredBy}，来源 PROBE）
 * 判断是否达间隔——探测与目录同步按来源（triggeredBy=PROBE/SYNC）独立判断周期，
 * 互不抑制。失败仅记录 error 日志，不向调度器抛异常。</p>
 */
/**
 * 定时装配开关：{@code gateway.catalog.probe.auto-enabled} 为 true（默认，matchIfMissing）
 * 时才注册本任务。测试环境显式关闭，避免 @SpringBootTest 启动后（initialDelay=0）
 * 立即触发真实探测。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "gateway.catalog.probe", name = "auto-enabled",
        havingValue = "true", matchIfMissing = true)
public class CatalogProbeTask {

    /** 上游列表探测编排服务 */
    private final CatalogProbeService probeService;

    /** 同步/探测日志 Repository（查询最近一次记录时间） */
    private final CatalogSyncLogRepository logRepository;

    /** 系统设置服务（读取探测开关与周期） */
    private final SystemSettingService settingService;

    /**
     * 定时检查是否需要执行上游列表探测
     *
     * <p>总开关或探测子开关关闭、未达探测间隔时跳过并输出 debug 日志；达到间隔时调用
     * {@link CatalogProbeService#probe()}，探测失败捕获 {@link RuntimeException}
     * 记录 error 日志，避免中断调度线程。
     * 周期判断仅取来源为 PROBE 的最近日志（{@link CatalogSyncLogRepository#findLatestByTriggeredBy}），
     * 目录同步（SYNC）日志不再参与探测周期判定。</p>
     */
    @Scheduled(fixedRate = 3600_000) // 每小时检查一次
    public void check() {
        if (!settingService.getBoolean("catalog.deprecation.enabled", true)
                || !settingService.getBoolean("catalog.deprecation.probe.enabled", true)) {
            log.debug("上游列表探测已关闭，跳过");
            return;
        }
        SyncInterval interval = settingService.getEnum(
                "catalog.deprecation.probe.interval", SyncInterval.class, SyncInterval.WEEKLY);
        long thresholdHours = switch (interval) {
            case DAILY -> 24;
            case WEEKLY -> 24 * 7;
            case MONTHLY -> 24 * 30;
        };
        Optional<CatalogSyncLog> latest = logRepository.findLatestByTriggeredBy("PROBE");
        boolean shouldProbe = latest.isEmpty()
                || latest.get().getSyncedAt() == null
                || latest.get().getSyncedAt().isBefore(Instant.now().minus(thresholdHours, ChronoUnit.HOURS));
        if (!shouldProbe) {
            log.debug("距上次探测未达间隔({}), 跳过", interval);
            return;
        }
        try {
            CatalogSyncReport report = probeService.probe();
            log.info("上游列表探测完成: updated={}", report.getUpdatedCount());
        } catch (RuntimeException e) {
            log.error("上游列表探测失败: {}", e.getMessage(), e);
        }
    }
}
