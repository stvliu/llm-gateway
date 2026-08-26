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

import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.CallLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
