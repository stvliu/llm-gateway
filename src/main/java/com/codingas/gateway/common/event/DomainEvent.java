package com.codingas.gateway.common.event;

import java.time.Instant;

/**
 * 领域事件基接口
 *
 * <p>所有领域事件实现此接口。</p>
 */
public interface DomainEvent {

    /**
     * 获取事件发生时间
     */
    Instant occurredOn();
}
