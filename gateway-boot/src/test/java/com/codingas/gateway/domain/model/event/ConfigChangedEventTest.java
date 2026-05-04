package com.codingas.gateway.domain.model.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigChangedEventTest {

    @Test
    @DisplayName("should create ConfigChangedEvent with all fields")
    void shouldCreateEventWithAllFields() {
        ConfigChangedEvent event = ConfigChangedEvent.of(
            ConfigChangedEvent.ConfigType.PROVIDER,
            ConfigChangedEvent.ChangeType.UPDATED,
            1L
        );

        assertThat(event.getConfigType()).isEqualTo(ConfigChangedEvent.ConfigType.PROVIDER);
        assertThat(event.getChangeType()).isEqualTo(ConfigChangedEvent.ChangeType.UPDATED);
        assertThat(event.getEntityId()).isEqualTo(1L);
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("should support all config types")
    void shouldSupportAllConfigTypes() {
        assertThat(ConfigChangedEvent.ConfigType.values())
            .containsExactly(
                ConfigChangedEvent.ConfigType.PROVIDER,
                ConfigChangedEvent.ConfigType.MODEL,
                ConfigChangedEvent.ConfigType.PROVIDER_API_KEY
            );
    }

    @Test
    @DisplayName("should support all change types")
    void shouldSupportAllChangeTypes() {
        assertThat(ConfigChangedEvent.ChangeType.values())
            .containsExactly(
                ConfigChangedEvent.ChangeType.CREATED,
                ConfigChangedEvent.ChangeType.UPDATED,
                ConfigChangedEvent.ChangeType.DELETED
            );
    }
}
