package com.codingas.gateway.application.resilience.event;

import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 转移事件监听器
 *
 * <p>接收 {@link FailoverOccurredEvent}，构造 {@link FailoverEvent} 实体并委托
 * {@link FailoverEventGateway#save} 持久化。</p>
 *
 * <p><b>AFTER_COMMIT 语义</b>：使用 {@link TransactionalEventListener} 监听
 * {@link TransactionPhase#AFTER_COMMIT}，确保仅在调用链所在事务提交成功后才持久化转移事件，
 * 避免事务回滚后仍写入孤儿事件。发布与持久化解耦，不阻塞 10k QPS 调用链
 * （设计见 design doc D12）。</p>
 *
 * <p><b>可靠性边界</b>：发布后持久化前进程崩溃则事件丢失（可观测性数据可接受，
 * 非计费/审计关键路径）。</p>
 *
 * <p>参照 {@code AuditEventListener} 的监听器范式。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailoverEventListener {

    private final FailoverEventGateway failoverEventGateway;

    /**
     * 处理转移发生事件，持久化为转移事件实体
     *
     * <p>从事件字段构造 {@link FailoverEvent} 实体(traceId/applicationId/fromXYZ/toXYZ/errorType/
     * decision/exhausted/occurredAt), 委托 Gateway 持久化。捕获异常仅记录日志不抛出,
     * 避免监听器异常影响调用链(可观测性数据丢失不应阻断业务)。</p>
     *
     * @param event 转移发生事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFailoverOccurred(FailoverOccurredEvent event) {
        try {
            FailoverEvent entity = toEntity(event);
            failoverEventGateway.save(entity);
            log.debug("持久化转移事件: traceId={}, fromChannel={}, toChannel={}, decision={}, exhausted={}",
                    event.traceId(), event.fromChannelId(), event.toChannelId(),
                    event.decision(), event.exhausted());
        } catch (Exception e) {
            // 可观测性数据持久化失败不应阻断业务，仅记录日志
            log.warn("转移事件持久化失败（可观测性数据，已忽略）: traceId={}, error={}",
                    event.traceId(), e.getMessage());
        }
    }

    /**
     * 事件字段构造转移事件实体
     */
    private FailoverEvent toEntity(FailoverOccurredEvent event) {
        FailoverEvent entity = new FailoverEvent();
        entity.setTraceId(event.traceId());
        entity.setApplicationId(event.applicationId());
        entity.setFromChannelId(event.fromChannelId());
        entity.setFromEndpointId(event.fromEndpointId());
        entity.setToChannelId(event.toChannelId());
        entity.setToEndpointId(event.toEndpointId());
        entity.setFromClusterId(event.fromClusterId());
        entity.setToClusterId(event.toClusterId());
        entity.setErrorType(event.errorType());
        entity.setDecision(event.decision());
        entity.setExhausted(event.exhausted());
        entity.setOccurredAt(event.occurredOn());
        return entity;
    }
}
