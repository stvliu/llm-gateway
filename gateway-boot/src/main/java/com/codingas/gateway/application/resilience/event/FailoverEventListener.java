package com.codingas.gateway.application.resilience.event;

import com.codingas.gateway.common.event.FailoverOccurredEvent;
import com.codingas.gateway.domain.resilience.entity.FailoverEvent;
import com.codingas.gateway.domain.resilience.gateway.FailoverEventGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 转移事件监听器
 *
 * <p>接收 {@link FailoverOccurredEvent}，构造 {@link FailoverEvent} 实体并委托
 * {@link FailoverEventGateway#save} 持久化。</p>
 *
 * <p><b>非事务监听</b>：使用 {@link EventListener}（而非 {@code @TransactionalEventListener}）。
 * 调用链 {@code ChatDispatchServiceImpl.dispatch} 无 {@code @Transactional}，整个请求处理不开启事务，
 * 原 {@code @TransactionalEventListener(AFTER_COMMIT)} 在无事务上下文时静默丢弃事件
 * （fallbackExecution 默认 false），导致转移事件全部丢失、可观测性功能失效。改为 {@link EventListener}
 * 后无事务上下文下事件仍被同步处理。</p>
 *
 * <p><b>异步说明</b>：未加 {@code @Async}——项目未配置 {@code @EnableAsync}，加了也是死注解。
 * 当前同步执行（发布即持久化），可观测性持久化开销在毫秒级，对 10k QPS 调用链影响可接受
 * （参照既有 {@code AuditEventListener} 范式，设计见 design doc D12）。</p>
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
    @EventListener
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
        entity.setErrorType(event.errorType());
        entity.setDecision(event.decision());
        entity.setExhausted(event.exhausted());
        entity.setOccurredAt(event.occurredOn());
        return entity;
    }
}
