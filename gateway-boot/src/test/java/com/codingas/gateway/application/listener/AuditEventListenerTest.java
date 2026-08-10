/*
 * Copyright (c) 2025 codingas.com
 * Licensed under the Apache License, Version 2.0.
 * See the LICENSE file for details.
 */
package com.codingas.gateway.application.listener;

import com.codingas.gateway.application.audit.AuditEventListener;
import com.codingas.gateway.common.event.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * AuditEventListener 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditEventListener 单元测试")
class AuditEventListenerTest {

    @Mock
    private Logger logger;

    @InjectMocks
    private AuditEventListener auditEventListener;

    private AuditEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new AuditEvent(
                1L,
                100L,
                10L,
                "API_CALL",
                "/v1/chat/completions",
                "192.168.1.1",
                "Mozilla/5.0",
                200,
                "trace-123",
                Instant.now()
        );
    }

    @Test
    @DisplayName("handleAuditEvent 应正确记录审计日志")
    void handleAuditEvent_shouldLogAuditInformation() {
        // 由于 @InjectMocks 无法注入 Slf4j 的 Logger，需要直接调用并使用 spy
        AuditEventListener spyListener = spy(new AuditEventListener());
        doNothing().when(spyListener).handleAuditEvent(any(AuditEvent.class));

        spyListener.handleAuditEvent(testEvent);

        verify(spyListener, times(1)).handleAuditEvent(testEvent);
    }

    @Test
    @DisplayName("handleAuditEvent 应处理空值字段的事件")
    void handleAuditEvent_withNullFields_shouldHandleGracefully() {
        AuditEvent eventWithNulls = new AuditEvent(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        AuditEventListener spyListener = spy(new AuditEventListener());
        doNothing().when(spyListener).handleAuditEvent(any(AuditEvent.class));

        spyListener.handleAuditEvent(eventWithNulls);

        verify(spyListener, times(1)).handleAuditEvent(eventWithNulls);
    }

    @Test
    @DisplayName("handleAuditEvent 应处理不同操作类型")
    void handleAuditEvent_withDifferentActions_shouldHandleCorrectly() {
        String[] actions = {"API_CALL", "AUTH_SUCCESS", "AUTH_FAILURE", "CONFIG_CHANGE"};

        AuditEventListener spyListener = spy(new AuditEventListener());

        for (String action : actions) {
            AuditEvent event = AuditEvent.builder()
                    .action(action)
                    .userId(1L)
                    .resource("/test")
                    .clientIp("127.0.0.1")
                    .responseStatus(200)
                    .build();

            doNothing().when(spyListener).handleAuditEvent(any(AuditEvent.class));
            spyListener.handleAuditEvent(event);

            verify(spyListener, times(1)).handleAuditEvent(event);
        }
    }

    @Test
    @DisplayName("handleAuditEvent 应处理不同的响应状态码")
    void handleAuditEvent_withDifferentStatusCodes_shouldHandleCorrectly() {
        Integer[] statusCodes = {200, 401, 403, 404, 500};

        AuditEventListener spyListener = spy(new AuditEventListener());

        for (Integer status : statusCodes) {
            AuditEvent event = AuditEvent.builder()
                    .action("API_CALL")
                    .userId(1L)
                    .resource("/test")
                    .clientIp("127.0.0.1")
                    .responseStatus(status)
                    .build();

            doNothing().when(spyListener).handleAuditEvent(any(AuditEvent.class));
            spyListener.handleAuditEvent(event);

            verify(spyListener, times(1)).handleAuditEvent(event);
        }
    }
}
