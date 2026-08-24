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
package com.codingas.gateway.auditdata.gateway;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.CallLog;
import com.codingas.gateway.audit.CallLogGateway;
import com.codingas.gateway.auditdata.repository.AuditLogRepository;
import com.codingas.gateway.auditdata.dataobject.AuditLogDo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuditGatewayImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditGatewayImpl 测试")
class AuditGatewayImplTest {

    @Mock
    private AuditLogRepository repository;

    @Mock
    private CallLogGateway callLogGateway;

    @InjectMocks
    private AuditGatewayImpl gateway;

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("保存审计日志成功")
        void save_validEntity_returnsSaved() {
            // given
            AuditLog entity = createTestEntity();
            AuditLogDo savedDo = createTestDo();

            when(repository.save(any())).thenReturn(savedDo);

            // when
            AuditLog result = gateway.save(entity);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAction()).isEqualTo("LOGIN");
            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("findByUserId 方法测试")
    class FindByUserIdTests {

        @Test
        @DisplayName("找到用户审计日志返回列表")
        void findByUserId_existingUser_returnsList() {
            // given
            AuditLogDo doEntity = createTestDo();
            when(repository.findByUserId(1L)).thenReturn(List.of(doEntity));

            // when
            List<AuditLog> result = gateway.findByUserId(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAction()).isEqualTo("LOGIN");
        }

        @Test
        @DisplayName("用户无审计日志返回空列表")
        void findByUserId_noLogs_returnsEmptyList() {
            // given
            when(repository.findByUserId(999L)).thenReturn(List.of());

            // when
            List<AuditLog> result = gateway.findByUserId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveCallLog 方法测试")
    class SaveCallLogTests {

        @Test
        @DisplayName("saveCallLog 委托给 CallLogGateway 保存")
        void saveCallLog_delegatesToCallLogGateway() {
            // given
            CallLog callLog = new CallLog();
            callLog.setTraceId("trace-1");
            CallLog saved = new CallLog();
            saved.setId(5L);
            saved.setTraceId("trace-1");
            when(callLogGateway.save(callLog)).thenReturn(saved);

            // when
            CallLog result = gateway.saveCallLog(callLog);

            // then
            assertThat(result).isSameAs(saved);
            assertThat(result.getId()).isEqualTo(5L);
            verify(callLogGateway).save(callLog);
        }
    }

    @Nested
    @DisplayName("null 防御转换测试")
    class NullConversionTests {

        @Test
        @DisplayName("保存 null 返回 null（toDo/toEntity 空分支）")
        void save_nullEntity_returnsNull() {
            // when
            AuditLog result = gateway.save(null);

            // then
            assertThat(result).isNull();
            verify(repository).save(null);
        }
    }

    // Helper methods
    private AuditLog createTestEntity() {
        AuditLog entity = new AuditLog();
        entity.setUserId(1L);
        entity.setAction("LOGIN");
        entity.setResource("/api/auth/login");
        entity.setResult("SUCCESS");
        entity.setIpAddress("192.168.1.1");
        return entity;
    }

    private AuditLogDo createTestDo() {
        AuditLogDo doEntity = new AuditLogDo();
        doEntity.setId(1L);
        doEntity.setUserId(1L);
        doEntity.setAction("LOGIN");
        doEntity.setResource("/api/auth/login");
        doEntity.setResult("SUCCESS");
        doEntity.setIpAddress("192.168.1.1");
        return doEntity;
    }
}
