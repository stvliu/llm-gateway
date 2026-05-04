package com.codingas.gateway.domain.model.event;

import com.codingas.gateway.common.event.DomainEvent;

import java.time.Instant;

/**
 * 配置变更事件
 *
 * <p>属于 model 领域，支持本地和远程两种传输方式。</p>
 */
public class ConfigChangedEvent implements DomainEvent {

    private final ConfigType configType;
    private final ChangeType changeType;
    private final Long entityId;
    private final Instant occurredOn;

    public enum ConfigType {
        PROVIDER,
        MODEL,
        PROVIDER_API_KEY
    }

    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED
    }

    public ConfigChangedEvent(ConfigType configType, ChangeType changeType, Long entityId) {
        this.configType = configType;
        this.changeType = changeType;
        this.entityId = entityId;
        this.occurredOn = Instant.now();
    }

    public ConfigType getConfigType() {
        return configType;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Long getEntityId() {
        return entityId;
    }

    @Override
    public Instant occurredOn() {
        return occurredOn;
    }

    public static ConfigChangedEvent of(ConfigType configType, ChangeType changeType, Long entityId) {
        return new ConfigChangedEvent(configType, changeType, entityId);
    }
}
