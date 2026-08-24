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
package com.codingas.gateway.resilience.failover;

import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.common.enums.FailoverDecision;
import com.codingas.gateway.common.enums.ProviderErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FailoverEventListener 单元测试
 *
 * <p>验证监听器接收 {@link FailoverOccurredEvent} 后，构造 {@link FailoverEvent} 实体
 * 并委托 {@link FailoverEventGateway#save} 持久化。事件字段到实体字段的映射完整，
 * 不阻塞调用链（监听器用 {@code @EventListener} 同步处理，调用链无事务故不能用事务监听；
 * 事务语义差异由 {@link FailoverEventListenerPublishTest} 覆盖，此处测持久化逻辑正确性）。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FailoverEventListener 测试")
class FailoverEventListenerTest {

    @Mock
    private FailoverEventGateway failoverEventGateway;

    @InjectMocks
    private FailoverEventListener listener;

    @Test
    @DisplayName("接收转移事件后构造实体并持久化（字段完整映射）")
    void handleFailoverOccurredEvent_persistsEntityWithAllFields() {
        Instant occurredOn = Instant.parse("2026-06-22T10:00:00Z");
        FailoverOccurredEvent event = new FailoverOccurredEvent(
                "trace-abc-123",
                7L,
                10L, 20L,
                11L, 21L,
                ProviderErrorType.AUTHENTICATION_ERROR,
                FailoverDecision.L1,
                false,
                occurredOn
        );
        FailoverEvent savedStub = new FailoverEvent();
        savedStub.setId(1L);
        when(failoverEventGateway.save(org.mockito.ArgumentMatchers.any(FailoverEvent.class)))
                .thenReturn(savedStub);

        listener.onFailoverOccurred(event);

        // 验证实体字段从事件字段完整构造
        ArgumentCaptor<FailoverEvent> captor = ArgumentCaptor.forClass(FailoverEvent.class);
        verify(failoverEventGateway).save(captor.capture());
        FailoverEvent captured = captor.getValue();
        assertThat(captured.getTraceId()).isEqualTo("trace-abc-123");
        assertThat(captured.getApplicationId()).isEqualTo(7L);
        assertThat(captured.getFromChannelId()).isEqualTo(10L);
        assertThat(captured.getFromEndpointId()).isEqualTo(20L);
        assertThat(captured.getToChannelId()).isEqualTo(11L);
        assertThat(captured.getToEndpointId()).isEqualTo(21L);
        assertThat(captured.getErrorType()).isEqualTo(ProviderErrorType.AUTHENTICATION_ERROR);
        assertThat(captured.getDecision()).isEqualTo(FailoverDecision.L1);
        assertThat(captured.isExhausted()).isFalse();
        assertThat(captured.getOccurredAt()).isEqualTo(occurredOn);
    }

    @Test
    @DisplayName("exhausted 事件（toChannelId 为 null）正确映射")
    void handleFailoverOccurredEvent_exhaustedEvent_mapsNullToFields() {
        Instant occurredOn = Instant.parse("2026-06-22T11:00:00Z");
        FailoverOccurredEvent event = new FailoverOccurredEvent(
                "trace-def-456",
                8L,
                10L, 20L,
                null, null,
                ProviderErrorType.UNKNOWN_ERROR,
                FailoverDecision.L1,
                true,
                occurredOn
        );
        when(failoverEventGateway.save(org.mockito.ArgumentMatchers.any(FailoverEvent.class)))
                .thenReturn(new FailoverEvent());

        listener.onFailoverOccurred(event);

        ArgumentCaptor<FailoverEvent> captor = ArgumentCaptor.forClass(FailoverEvent.class);
        verify(failoverEventGateway).save(captor.capture());
        FailoverEvent captured = captor.getValue();
        assertThat(captured.getToChannelId()).isNull();
        assertThat(captured.getToEndpointId()).isNull();
        assertThat(captured.isExhausted()).isTrue();
        assertThat(captured.getDecision()).isEqualTo(FailoverDecision.L1);
    }

    @Test
    @DisplayName("持久化异常时吞掉异常不阻断调用链")
    void handleFailoverOccurredEvent_saveThrows_swallowsException() {
        Instant occurredOn = Instant.parse("2026-06-22T12:00:00Z");
        FailoverOccurredEvent event = new FailoverOccurredEvent(
                "trace-save-fail",
                9L,
                10L, 20L,
                11L, 21L,
                ProviderErrorType.UNKNOWN_ERROR,
                FailoverDecision.L1,
                false,
                occurredOn
        );
        when(failoverEventGateway.save(org.mockito.ArgumentMatchers.any(FailoverEvent.class)))
                .thenThrow(new RuntimeException("数据库不可用"));

        assertThatCode(() -> listener.onFailoverOccurred(event)).doesNotThrowAnyException();
        verify(failoverEventGateway).save(org.mockito.ArgumentMatchers.any(FailoverEvent.class));
    }
}
