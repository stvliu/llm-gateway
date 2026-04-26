package com.codingas.gateway.core.domain.event;

import java.time.Instant;

/**
 * 领域事件基接口
 *
 * <p>所有领域事件实现此接口。</p>
 *
 * <p>设计原则：
 * <ul>
 *   <li>领域事件是 DDD 中的核心概念，用于解耦聚合根之间的协作</li>
 *   <li>事件是 immutable 的 POJO</li>
 *   <li>事件包含发生时间、涉及实体等元数据</li>
 * </ul>
 */
public interface DomainEvent {

    /**
     * 获取事件发生时间
     */
    Instant occurredOn();
}
