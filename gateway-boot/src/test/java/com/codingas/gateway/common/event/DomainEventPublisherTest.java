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
package com.codingas.gateway.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

class DomainEventPublisherTest {

    @Test
    @DisplayName("DomainEventPublisher should be a functional interface")
    void shouldBeFunctionalInterface() {
        assertThatCode(() -> {
            DomainEventPublisher publisher = new TestPublisher();
            publisher.publish(new TestEvent());
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DomainEventPublisher should be usable as anonymous class")
    void shouldBeUsableAsAnonymousClass() {
        DomainEventPublisher publisher = new DomainEventPublisher() {
            @Override
            public <T extends DomainEvent> void publish(T event) {
                // no-op for testing
            }
        };
        assertThatCode(() -> publisher.publish(new TestEvent()))
            .doesNotThrowAnyException();
    }

    static class TestEvent implements DomainEvent {
        @Override
        public Instant occurredOn() {
            return Instant.now();
        }
    }

    static class TestPublisher implements DomainEventPublisher {
        @Override
        public <T extends DomainEvent> void publish(T event) {
            // no-op for testing
        }
    }
}
