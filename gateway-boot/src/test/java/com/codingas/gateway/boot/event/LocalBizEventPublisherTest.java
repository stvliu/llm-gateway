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
package com.codingas.gateway.boot.event;

import com.codingas.gateway.common.event.BizEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.mockito.Mockito.*;

/**
 * LocalBizEventPublisher 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalBizEventPublisher 测试")
class LocalBizEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private LocalBizEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new LocalBizEventPublisher(applicationEventPublisher);
    }

    @Nested
    @DisplayName("publish 方法测试")
    class PublishTests {

        @Test
        @DisplayName("成功发布事件")
        void publish_success() {
            // Given
            TestBizEvent event = new TestBizEvent("test-id", "test-data");

            // When
            publisher.publish(event);

            // Then
            verify(applicationEventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("发布多个事件")
        void publish_multipleEvents() {
            // Given
            TestBizEvent event1 = new TestBizEvent("id-1", "data-1");
            TestBizEvent event2 = new TestBizEvent("id-2", "data-2");

            // When
            publisher.publish(event1);
            publisher.publish(event2);

            // Then
            verify(applicationEventPublisher).publishEvent(event1);
            verify(applicationEventPublisher).publishEvent(event2);
        }

        @Test
        @DisplayName("验证事件调用次数")
        void publish_verifyCallCount() {
            // Given
            TestBizEvent event = new TestBizEvent("id", "data");

            // When
            publisher.publish(event);
            publisher.publish(event);

            // Then
            verify(applicationEventPublisher, times(2)).publishEvent(event);
        }
    }

    /**
     * 测试用业务事件
     */
    static class TestBizEvent implements BizEvent {
        private final String eventId;
        private final String data;
        private final Instant occurredOn;

        public TestBizEvent(String eventId, String data) {
            this.eventId = eventId;
            this.data = data;
            this.occurredOn = Instant.now();
        }

        public String getEventId() {
            return eventId;
        }

        public String getData() {
            return data;
        }

        @Override
        public Instant occurredOn() {
            return occurredOn;
        }

        @Override
        public String toString() {
            return "TestBizEvent{eventId='" + eventId + "', data='" + data + "'}";
        }
    }
}
