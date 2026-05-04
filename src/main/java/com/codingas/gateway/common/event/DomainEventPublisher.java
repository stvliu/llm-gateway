package com.codingas.gateway.common.event;

/**
 * 领域事件发布器
 *
 * <p>通用接口，支持本地和远程两种实现。</p>
 */
@FunctionalInterface
public interface DomainEventPublisher {

    /**
     * 发布领域事件
     *
     * @param event 领域事件
     * @param <T> 事件类型
     */
    <T extends DomainEvent> void publish(T event);
}
