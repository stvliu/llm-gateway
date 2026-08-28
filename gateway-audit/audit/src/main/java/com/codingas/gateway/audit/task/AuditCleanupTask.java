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
package com.codingas.gateway.audit.task;

import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.settings.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 审计日志定时清理任务
 *
 * <p>每日凌晨 3:00 执行：读取系统设置 {@code audit.retention.days}（默认 90 天）
 * 计算保留截止时间，删除 createdAt 早于截止时间的审计日志。
 * {@code @EnableScheduling} 已由 {@code GatewayApplication} 开启。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditCleanupTask {

    /** 审计日志保留天数配置键 */
    private static final String RETENTION_DAYS_KEY = "audit.retention.days";

    /** 默认保留天数 */
    private static final int DEFAULT_RETENTION_DAYS = 90;

    private final AuditLogRepository auditLogRepository;
    private final SystemSettingService settingService;

    /**
     * 执行每日审计日志清理
     *
     * <p>按配置的保留天数计算截止时间（now - 保留天数），删除早于该时间的审计日志并记录结果。</p>
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanup() {
        int retentionDays = settingService.getInt(RETENTION_DAYS_KEY, DEFAULT_RETENTION_DAYS);
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = auditLogRepository.deleteBefore(cutoff);
        log.info("审计日志定时清理完成: 保留 {} 天, 删除 {} 条", retentionDays, deleted);
    }
}
