package com.codingas.gateway.application.degradation;

import com.codingas.gateway.common.event.DomainEvent;
import com.codingas.gateway.domain.supply.enums.ProviderErrorType;

import java.time.Instant;

/**
 * 降级事件
 *
 * <p>当模型降级发生时发布。</p>
 */
public class DegradationEvent implements DomainEvent {

    private final String traceId;
    private final Long userId;
    private final String originalModel;
    private final String fallbackModel;
    private final ProviderErrorType reason;
    private final int chainStep;
    private final Instant triggeredAt;

    public DegradationEvent(String traceId, Long userId, String originalModel,
                            String fallbackModel, ProviderErrorType reason,
                            int chainStep, Instant triggeredAt) {
        this.traceId = traceId;
        this.userId = userId;
        this.originalModel = originalModel;
        this.fallbackModel = fallbackModel;
        this.reason = reason;
        this.chainStep = chainStep;
        this.triggeredAt = triggeredAt;
    }

    @Override
    public Instant occurredOn() { return triggeredAt; }

    public String getTraceId() { return traceId; }
    public Long getUserId() { return userId; }
    public String getOriginalModel() { return originalModel; }
    public String getFallbackModel() { return fallbackModel; }
    public ProviderErrorType getReason() { return reason; }
    public int getChainStep() { return chainStep; }
    public Instant getTriggeredAt() { return triggeredAt; }
}
