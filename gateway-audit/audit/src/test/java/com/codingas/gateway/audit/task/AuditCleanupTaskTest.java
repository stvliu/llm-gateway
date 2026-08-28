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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuditCleanupTask 单元测试
 *
 * <p>验证每日定时清理任务按保留天数计算截止时间并委托 {@link AuditLogRepository} 删除。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditCleanupTask 测试")
class AuditCleanupTaskTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SystemSettingService settingService;

    private AuditCleanupTask task;

    @BeforeEach
    void setUp() {
        task = new AuditCleanupTask(auditLogRepository, settingService);
    }

    @Test
    @DisplayName("每日清理：按保留天数删除截止时间前的审计日志")
    void cleanup_deletesBeforeCutoff() {
        // given
        when(settingService.getInt("audit.retention.days", 90)).thenReturn(90);
        when(auditLogRepository.deleteBefore(any(Instant.class))).thenReturn(42);

        // when
        task.cleanup();

        // then
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(auditLogRepository).deleteBefore(captor.capture());
        assertThat(captor.getValue()).isBefore(Instant.now());
        assertThat(captor.getValue()).isAfter(Instant.now().minus(91, ChronoUnit.DAYS));
    }
}
