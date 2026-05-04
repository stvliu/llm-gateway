package com.codingas.gateway.infrastructure.config;

import com.codingas.gateway.common.event.DomainEventPublisher;
import com.codingas.gateway.domain.model.event.ConfigChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 配置事件发布
 *
 * <p>提供统一的事件发布入口，属于技术基础设施。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigEventPublisher {

    private final DomainEventPublisher eventPublisher;

    /**
     * 发布配置变更事件
     *
     * @param configType 配置类型
     * @param changeType 变更类型
     * @param entityId 实体 ID
     */
    public void publishConfigChanged(ConfigChangedEvent.ConfigType configType,
                                     ConfigChangedEvent.ChangeType changeType,
                                     Long entityId) {
        ConfigChangedEvent event = ConfigChangedEvent.of(configType, changeType, entityId);
        log.info("Publishing config event: {}", event);
        eventPublisher.publish(event);
    }
}
