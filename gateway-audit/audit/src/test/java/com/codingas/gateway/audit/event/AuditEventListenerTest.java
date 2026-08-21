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

import com.codingas.gateway.common.event.AuditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * AuditEventListener 单元测试
 */
@DisplayName("AuditEventListener 测试")
class AuditEventListenerTest {

    @Test
    @DisplayName("处理审计事件不抛出异常")
    void handleAuditEvent_noException() {
        // Given
        AuditEventListener listener = new AuditEventListener();
        AuditEvent event = AuditEvent.builder()
                .userId(1L)
                .apiKeyId(1L)
                .teamId(1L)
                .action("LOGIN")
                .resource("/api/auth/login")
                .clientIp("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .responseStatus(200)
                .traceId("trace-123")
                .occurredOn(Instant.now())
                .build();

        // When & Then - 不应抛出异常
        listener.handleAuditEvent(event);
    }

    @Test
    @DisplayName("处理不同类型的审计事件")
    void handleAuditEvent_differentActionTypes() {
        // Given
        AuditEventListener listener = new AuditEventListener();

        // When & Then - 测试不同类型的审计事件
        AuditEvent loginEvent = createAuditEvent("LOGIN", 1L, "/auth", "192.168.1.1", 200);
        AuditEvent logoutEvent = createAuditEvent("LOGOUT", 1L, "/auth", "192.168.1.1", 200);
        AuditEvent apiKeyEvent = createAuditEvent("API_KEY_CREATE", 1L, "/api/keys", "192.168.1.1", 201);
        AuditEvent failedEvent = createAuditEvent("LOGIN", 2L, "/auth", "10.0.0.1", 401);

        listener.handleAuditEvent(loginEvent);
        listener.handleAuditEvent(logoutEvent);
        listener.handleAuditEvent(apiKeyEvent);
        listener.handleAuditEvent(failedEvent);
    }

    private AuditEvent createAuditEvent(String action, Long userId, String resource, String clientIp, int status) {
        return AuditEvent.builder()
                .userId(userId)
                .apiKeyId(1L)
                .teamId(1L)
                .action(action)
                .resource(resource)
                .clientIp(clientIp)
                .userAgent("Mozilla/5.0")
                .responseStatus(status)
                .traceId("trace-" + action.toLowerCase())
                .occurredOn(Instant.now())
                .build();
    }
}
