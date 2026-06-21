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
 *
 * <p><b>profile 覆盖</b>：覆盖 local/dev/standalone/test/prod 全部 profile。单实例架构下，
 * 本地 ApplicationEvent 发布可接受（与既有 ChatDispatchServiceImpl 审计事件发布行为一致）。
 * 生产 profile（prod）也启用此 bean，避免 {@code ChannelFailoverInvoker}（构造注入
 * {@link com.codingas.gateway.common.event.DomainEventPublisher}）与 {@code ChatDispatchServiceImpl}
 * 在生产启动时因无 {@link DomainEventPublisher} 实现 bean 而抛
 * {@code NoSuchBeanDefinitionException}（既有架构缺陷的修复）。</p>
 */
@Component
@Profile({"local", "dev", "standalone", "test", "prod"})
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
