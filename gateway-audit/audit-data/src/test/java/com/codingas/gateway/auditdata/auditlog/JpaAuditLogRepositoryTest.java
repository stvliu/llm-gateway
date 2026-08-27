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
package com.codingas.gateway.auditdata.auditlog;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditLogQuery;
import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.CallLogRepository;
import com.codingas.gateway.common.dto.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * JpaAuditLogRepository 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaAuditLogRepository 测试")
class JpaAuditLogRepositoryTest {

    @Mock
    private CallLogRepository callLogRepository;

    @Mock
    private AuditLogJpaRepository auditLogJpaRepository;

    @InjectMocks
    private JpaAuditLogRepository gateway;

    @Test
    @DisplayName("saveCallLog 委托给 CallLogRepository 保存")
    void saveCallLog_delegatesToCallLogGateway() {
        // given
        CallLog callLog = new CallLog();
        callLog.setTraceId("trace-1");
        CallLog saved = new CallLog();
        saved.setId(5L);
        saved.setTraceId("trace-1");
        when(callLogRepository.save(callLog)).thenReturn(saved);

        // when
        CallLog result = gateway.saveCallLog(callLog);

        // then
        assertThat(result).isSameAs(saved);
        assertThat(result.getId()).isEqualTo(5L);
        verify(callLogRepository).save(callLog);
    }

    @Test
    @DisplayName("saveAuditLog 经 AuditLogJpaRepository 落库并映射返回")
    void saveAuditLog_persistsAndMapsBack() {
        // given
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(1L);
        auditLog.setAction("POST /api/v1/channels");
        auditLog.setResource("/api/v1/channels");
        auditLog.setResult("SUCCESS");
        auditLog.setIpAddress("192.168.1.1");

        AuditLogDo savedDo = new AuditLogDo();
        savedDo.setId(9L);
        savedDo.setUserId(1L);
        savedDo.setAction("POST /api/v1/channels");
        savedDo.setResource("/api/v1/channels");
        savedDo.setResult("SUCCESS");
        savedDo.setIpAddress("192.168.1.1");
        savedDo.setCreatedAt(Instant.parse("2026-08-27T10:00:00Z"));
        when(auditLogJpaRepository.save(org.mockito.ArgumentMatchers.any(AuditLogDo.class)))
                .thenReturn(savedDo);

        // when
        AuditLog result = gateway.saveAuditLog(auditLog);

        // then
        ArgumentCaptor<AuditLogDo> captor = ArgumentCaptor.forClass(AuditLogDo.class);
        verify(auditLogJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("POST /api/v1/channels");

        assertThat(result.getId()).isEqualTo(9L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getAction()).isEqualTo("POST /api/v1/channels");
        assertThat(result.getResource()).isEqualTo("/api/v1/channels");
        assertThat(result.getResult()).isEqualTo("SUCCESS");
        assertThat(result.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2026-08-27T10:00:00Z"));
    }

    @Test
    @DisplayName("findAuditLogs 支持分页并按时间倒序（最新在前）")
    void findAuditLogs_pagesAndSortsByCreatedAtDesc() {
        // given：乱序三条
        when(auditLogJpaRepository.findAll()).thenReturn(List.of(
                do_(1L, 1L, "POST /api/v1/channels", "SUCCESS", "2026-08-27T08:00:00Z"),
                do_(2L, 2L, "POST /api/v1/users", "SUCCESS", "2026-08-27T10:00:00Z"),
                do_(3L, 1L, "DELETE /api/v1/users/1", "SUCCESS", "2026-08-27T09:00:00Z")
        ));
        AuditLogQuery query = new AuditLogQuery();
        query.setPage(1);
        query.setLimit(2);

        // when
        PageResponse<AuditLog> page = gateway.findAuditLogs(query);

        // then
        assertThat(page.getPagination().getTotal()).isEqualTo(3);
        assertThat(page.getItems()).hasSize(2);
        // 时间倒序：10:00 → 09:00
        assertThat(page.getItems().get(0).getCreatedAt())
                .isEqualTo(Instant.parse("2026-08-27T10:00:00Z"));
        assertThat(page.getItems().get(1).getCreatedAt())
                .isEqualTo(Instant.parse("2026-08-27T09:00:00Z"));
    }

    @Test
    @DisplayName("findAuditLogs 支持按操作人/动作/结果/时间范围筛选")
    void findAuditLogs_filtersByCriteria() {
        // given
        when(auditLogJpaRepository.findAll()).thenReturn(List.of(
                do_(1L, 1L, "POST /api/v1/channels", "SUCCESS", "2026-08-27T08:00:00Z"),
                do_(2L, 2L, "POST /api/v1/channels", "SUCCESS", "2026-08-27T09:00:00Z"),
                do_(3L, 1L, "DELETE /api/v1/users/1", "FAILURE", "2026-08-27T10:00:00Z")
        ));
        AuditLogQuery query = new AuditLogQuery();
        query.setPage(1);
        query.setLimit(20);
        query.setUserId(1L);
        query.setAction("channels");
        query.setResult("SUCCESS");
        query.setStartTime(Instant.parse("2026-08-27T07:00:00Z"));
        query.setEndTime(Instant.parse("2026-08-27T23:59:59Z"));

        // when
        PageResponse<AuditLog> page = gateway.findAuditLogs(query);

        // then：仅 userId=1 且动作含 channels 且 SUCCESS 的记录 1
        assertThat(page.getPagination().getTotal()).isEqualTo(1);
        assertThat(page.getItems()).singleElement()
                .satisfies(l -> {
                    assertThat(l.getId()).isEqualTo(1L);
                    assertThat(l.getUserId()).isEqualTo(1L);
                    assertThat(l.getAction()).isEqualTo("POST /api/v1/channels");
                });
    }

    private AuditLogDo do_(Long id, Long userId, String action, String result, String createdAt) {
        AuditLogDo do_ = new AuditLogDo();
        do_.setId(id);
        do_.setUserId(userId);
        do_.setAction(action);
        do_.setResource(action);
        do_.setResult(result);
        do_.setIpAddress("127.0.0.1");
        do_.setCreatedAt(Instant.parse(createdAt));
        return do_;
    }
}
