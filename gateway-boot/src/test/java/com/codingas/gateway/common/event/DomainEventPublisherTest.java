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
