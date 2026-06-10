package com.codingas.gateway.application.degradation;

import com.codingas.gateway.common.event.DomainEvent;

import java.time.Instant;

/**
 * 降级恢复事件
 *
 * <p>当降级中的模型恢复时发布。</p>
 */
public class DegradationRecoveredEvent implements DomainEvent {

    private final String model;
    private final Instant recoveredAt;

    public DegradationRecoveredEvent(String model, Instant recoveredAt) {
        this.model = model;
        this.recoveredAt = recoveredAt;
    }

    @Override
    public Instant occurredOn() { return recoveredAt; }

    public String getModel() { return model; }
    public Instant getRecoveredAt() { return recoveredAt; }
}
