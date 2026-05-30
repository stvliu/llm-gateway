package com.codingas.gateway.infrastructure.event;

import com.codingas.gateway.common.event.DomainEvent;
import com.codingas.gateway.common.event.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 本地事件发布器
 *
 * <p>使用 Spring ApplicationEvent，适用于单实例部署。</p>
 */
@Component
@Profile({"local", "dev", "standalone", "test"})
@Slf4j
@RequiredArgsConstructor
public class LocalDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public <T extends DomainEvent> void publish(T event) {
        log.debug("Publishing local event: {}", event);
        eventPublisher.publishEvent(event);
    }
}
