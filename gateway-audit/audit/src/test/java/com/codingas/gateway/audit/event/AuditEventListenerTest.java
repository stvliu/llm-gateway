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
package com.codingas.gateway.audit.event;

import com.codingas.gateway.audit.AuditLog;
import com.codingas.gateway.audit.AuditLogRepository;
import com.codingas.gateway.common.event.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * AuditEventListener 单元测试
 *
 * <p>验证审计事件正确落库（result 由响应状态推导、未认证主体 userId=0、
 * 落库失败不抛异常）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventListener 单元测试")
class AuditEventListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditEventListener auditEventListener;

    @Test
    @DisplayName("成功事件（2xx）落库为 SUCCESS，字段完整透传")
    void handleAuditEvent_success_savesLogWithSuccessResult() {
        // given
        AuditEvent event = AuditEvent.builder()
                .userId(1L)
                .action("POST /api/v1/channels")
                .resource("/api/v1/channels")
                .clientIp("192.168.1.1")
                .responseStatus(200)
                .build();

        // when
        auditEventListener.handleAuditEvent(event);

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAuditLog(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getAction()).isEqualTo("POST /api/v1/channels");
        assertThat(saved.getResource()).isEqualTo("/api/v1/channels");
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("失败事件（4xx/5xx）落库为 FAILURE，未认证主体 userId 归一为 0")
    void handleAuditEvent_failure_resultIsFailureAndUserIdNormalized() {
        // given：登录失败场景——无 userId、401
        AuditEvent event = AuditEvent.builder()
                .userId(null)
                .action("POST /api/v1/auth/login")
                .resource("/api/v1/auth/login")
                .clientIp("1.2.3.4")
                .responseStatus(401)
                .build();

        // when
        auditEventListener.handleAuditEvent(event);

        // then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).saveAuditLog(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getUserId()).isZero();
        assertThat(saved.getResult()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("落库失败仅记录日志，不向外抛异常")
    void handleAuditEvent_saveThrows_swallowsException() {
        // given
        AuditEvent event = AuditEvent.builder()
                .userId(1L)
                .action("DELETE /api/v1/users/1")
                .clientIp("10.0.0.1")
                .responseStatus(500)
                .build();
        doThrow(new RuntimeException("db down"))
                .when(auditLogRepository).saveAuditLog(any(AuditLog.class));

        // when/then：不抛异常
        assertThatCode(() -> auditEventListener.handleAuditEvent(event))
                .doesNotThrowAnyException();
    }
}
